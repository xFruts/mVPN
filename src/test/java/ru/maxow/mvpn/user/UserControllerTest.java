package ru.maxow.mvpn.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponseDto userResponseDto;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        userResponseDto = new UserResponseDto(1L, "Test User", "key", "USER");
        userRequestDto = new UserRequestDto("Test User", 12345L);
    }

    @Test
    void getUsers_shouldReturnPageOfUsers() throws Exception {
        Page<UserResponseDto> userPage = new PageImpl<>(Collections.singletonList(userResponseDto));
        when(userService.findAll(any(PageRequest.class))).thenReturn(userPage);

        mockMvc.perform(get("/v1/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].fullName").value("Test User"));
    }

    @Test
    void saveUser_shouldCreateAndReturnUser() throws Exception {
        when(userService.createUser(any(UserRequestDto.class))).thenReturn(userResponseDto);

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void updateUser_shouldUpdateAndReturnUser() throws Exception {
        when(userService.updateUser(eq(1L), any(UserRequestDto.class))).thenReturn(userResponseDto);

        mockMvc.perform(put("/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void updateUser_whenUserNotFound_shouldReturnNotFound() throws Exception {
        when(userService.updateUser(eq(1L), any(UserRequestDto.class))).thenThrow(new NotFoundException("User", 1L));

        mockMvc.perform(put("/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUserById(1L);

        mockMvc.perform(delete("/v1/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_whenUserNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("User", 1L)).when(userService).deleteUserById(1L);

        mockMvc.perform(delete("/v1/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserRole_shouldUpdateAndReturnUser() throws Exception {
        UpdateUserRoleRequest roleRequest = new UpdateUserRoleRequest("ADMIN");
        when(userService.updateUserRole(1L, "ADMIN")).thenReturn(userResponseDto);

        mockMvc.perform(patch("/v1/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}

