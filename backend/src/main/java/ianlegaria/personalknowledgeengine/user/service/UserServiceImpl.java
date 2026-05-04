package ianlegaria.personalknowledgeengine.user.service;

import ianlegaria.personalknowledgeengine.common.exception.DuplicateResourceException;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.user.dto.CreateUserRequest;
import ianlegaria.personalknowledgeengine.user.dto.UserResponse;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();

        UserEntity saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    private UserResponse toResponse(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
