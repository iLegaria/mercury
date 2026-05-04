package ianlegaria.personalknowledgeengine.user.service;

import ianlegaria.personalknowledgeengine.user.dto.CreateUserRequest;
import ianlegaria.personalknowledgeengine.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);
}
