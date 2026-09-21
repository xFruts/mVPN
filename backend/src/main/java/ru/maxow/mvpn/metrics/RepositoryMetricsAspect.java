package ru.maxow.mvpn.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepositoryMetricsAspect {

  MeterRegistry meterRegistry;
  ConcurrentMap<String, String> operationTypeCache = new ConcurrentHashMap<>();

  @Around("execution(* ru.maxow.mvpn..*Repository.*(..))")
  public Object measureRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    String repositoryName = resolveRepositoryName(joinPoint, signature);
    String methodName = method.getName();
    String operationType = operationTypeCache.computeIfAbsent(methodName, this::determineOperationType);

    Timer.Sample sample = Timer.start(meterRegistry);
    String outcome = "success";

    try {
      return joinPoint.proceed();
    } catch (Throwable ex) {
      outcome = "error";
      recordError(repositoryName, methodName, ex.getClass().getSimpleName());
      throw ex;
    } finally {
      Timer timer = getTimer(repositoryName, methodName, operationType, outcome);
      sample.stop(timer);
    }
  }

  private String resolveRepositoryName(ProceedingJoinPoint joinPoint, MethodSignature signature) {
    Class<?> declaringType = signature.getDeclaringType();
    if (declaringType != null && declaringType.getSimpleName().endsWith("Repository")
        && !declaringType.getName().startsWith("org.springframework.data")) {
      return declaringType.getSimpleName();
    }
    Object proxy = joinPoint.getThis();
    if (proxy != null) {
      for (Class<?> iface : proxy.getClass().getInterfaces()) {
        if (iface.getSimpleName().endsWith("Repository")
            && !iface.getName().startsWith("org.springframework.data")) {
          return iface.getSimpleName();
        }
      }
    }
    return declaringType != null ? declaringType.getSimpleName() : "UnknownRepository";
  }

  private Timer getTimer(String repo, String method, String op, String outcome) {
    return Timer.builder("repository.method.duration")
        .description("Repository method execution time")
        .tag("repository", repo)
        .tag("method", method)
        .tag("operation", op)
        .tag("outcome", outcome)
        .publishPercentileHistogram()
        .register(meterRegistry);
  }

  private void recordError(String repo, String method, String exception) {
    Counter.builder("repository.errors")
        .description("Total repository execution errors")
        .tag("repository", repo)
        .tag("method", method)
        .tag("exception", exception)
        .register(meterRegistry)
        .increment();
  }

  private String determineOperationType(String methodName) {
    String lower = methodName.toLowerCase();
    if (lower.startsWith("find") || lower.startsWith("get")
        || lower.startsWith("count") || lower.startsWith("exists")
        || lower.startsWith("query") || lower.startsWith("search")
        || lower.startsWith("stream") || lower.startsWith("read")) {
      return "read";
    }
    if (lower.startsWith("save") || lower.startsWith("update") || lower.startsWith("insert")) {
      return "write";
    }
    if (lower.startsWith("delete") || lower.startsWith("remove")) {
      return "delete";
    }
    return "other";
  }
}
