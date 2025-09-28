package ru.maxow.mvpn.user;

import java.io.InputStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.util.exception.NotFoundException;


/**
 * REST controller for managing users.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
  UserService userService;

  /**
   * Retrieves a paginated list of users.
   *
   * @param pageable pagination information
   * @return a page of UserResponseDto
   */
  @GetMapping
  public ResponseEntity<Page<UserResponseDto>> getUsers(Pageable pageable) {
    Page<UserResponseDto> users = userService.findAll(pageable);
    return ResponseEntity.ok(users);
  }

  /**
   * Saves a new user.
   *
   * @param userRequestDto the user data to save
   * @return the created user
   */
  @PostMapping
  public ResponseEntity<UserResponseDto> saveUser(
      @RequestBody UserRequestDto userRequestDto) {
    UserResponseDto createdUser = userService.createUser(userRequestDto);
    log.info("User with ID: {} created: ", createdUser.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  /**
   * Updates an existing user.
   *
   * @param userId the ID of the user to update
   * @param userRequestDto the updated user data
   * @return the updated user
   */
  @PutMapping("/{userId}")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable Long userId, @RequestBody UserRequestDto userRequestDto) {
    UserResponseDto updatedUser = userService.updateUser(userId, userRequestDto);
    log.info("User with ID: {} updated: ", updatedUser.id());
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Deletes a user by ID.
   *
   * @param userId the ID of the user to delete
   * @return a response entity with no content
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId) {
    userService.deleteUserById(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates the role of a user.
   *
   * @param userId the ID of the user to update
   * @param request the new role data
   * @return the updated user
   */
  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserResponseDto> updateUserRole(
      @PathVariable Long userId, @RequestBody UpdateUserRoleRequest request) {
    UserResponseDto updatedUser = userService.updateUserRole(userId, request.role());
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Downloads the configuration file for a user.
   *
   * @param id the ID of the user
   * @return the configuration file as a resource
   */
  @GetMapping("/{id}/config")
  public ResponseEntity<Resource> downloadConfigFile(@PathVariable Long id) {
    try {
      InputStream inputStream = userService.downloadConfigFile(id);
      InputStreamResource resource = new InputStreamResource(inputStream);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"config.ovpn\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(resource);
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error downloading file for user {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Uploads a configuration file for a user.
   *
   * @param id the ID of the user
   * @param file the configuration file to upload
   * @return a response entity indicating the result of the upload
   */
  @PostMapping("/{id}/config")
  public ResponseEntity<?> uploadConfigFile(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    try {
      userService.attachConfigFile(id, file);
      return ResponseEntity.ok().body("File uploaded successfully");
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Could not upload the file: " + e.getMessage());
    }
  }

  /**
   * Deletes the configuration file for a user.
   *
   * @param id the ID of the user
   * @return a response entity indicating the result of the deletion
   */
  @DeleteMapping("/{id}/config")
  public ResponseEntity<Void> deleteConfigFile(@PathVariable Long id) {
    try {
      userService.deleteConfigFile(id);
      return ResponseEntity.noContent().build();
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error deleting file for user {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
