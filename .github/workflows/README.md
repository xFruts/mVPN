# CI/CD пайплайны

В этой директории находятся базовые workflow в enterprise-стиле для проекта.

## Workflow

- `ci.yml`
  - `backend-verify`: обязательный базовый Maven gate (`verify`) для backend.
  - `backend-quality-preview`: preview строгого backend-профиля (`-Pci`) в неблокирующем режиме.
  - `frontend-lint-build`: обязательный gate для frontend (lint + build).

- `security.yml`
  - `dependency-review`: проверяет изменения зависимостей в pull request.
  - `codeql`: статический анализ для Java и JS/TS.
  - `backend-dependency-check`: OWASP CVE-скан зависимостей с HTML-отчетом в артефактах.

## Почему строгий backend gate пока только в preview

Текущий строгий порог JaCoCo в `backend/pom.xml` (`LINE 0.55`, `BRANCH 0.35` в профиле `ci`)
выше фактического покрытия. Неблокирующий режим позволяет команде повышать покрытие,
не останавливая все PR.

Когда покрытие стабилизируется, сделайте `backend-quality-preview` обязательной проверкой
в branch protection.

## Рекомендуемые обязательные проверки в branch protection

- `backend-verify`
- `frontend-lint-build`
- `dependency-review`
- `codeql (java-kotlin)`
- `codeql (javascript-typescript)`
