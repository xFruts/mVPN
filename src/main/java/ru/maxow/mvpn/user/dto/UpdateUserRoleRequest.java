package ru.maxow.mvpn.user.dto;

/**
 * Request to update a user's role.
 *
 * @param role the new role for the user
 */
public record UpdateUserRoleRequest(
    String role
) {}
