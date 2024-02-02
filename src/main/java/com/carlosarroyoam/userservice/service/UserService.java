package com.carlosarroyoam.userservice.service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import org.jboss.logging.Logger;

import com.carlosarroyoam.userservice.constant.AppMessages;
import com.carlosarroyoam.userservice.dto.ChangePasswordDto;
import com.carlosarroyoam.userservice.dto.CreateUserDto;
import com.carlosarroyoam.userservice.dto.UpdateUserDto;
import com.carlosarroyoam.userservice.dto.UserDto;
import com.carlosarroyoam.userservice.mapper.UserMapper;
import com.carlosarroyoam.userservice.model.User;
import com.carlosarroyoam.userservice.repository.UserRepository;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
@Transactional
public class UserService {

	private static final Logger LOG = Logger.getLogger(UserService.class);
	private final UserRepository userRepository;
	private final UserMapper mapper;
	private final Clock clock;

	@Inject
	public UserService(final UserRepository userRepository, final UserMapper mapper, final Clock clock) {
		this.userRepository = userRepository;
		this.mapper = mapper;
		this.clock = clock;
	}

	public List<UserDto> findAll() {
		return mapper.toDtos(userRepository.listAll());
	}

	public UserDto findById(Long userId) {
		User userById = userRepository.findByIdOptional(userId).orElseThrow(() -> {
			LOG.errorf(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_DETAILED_MESSAGE, userId);
			return new NotFoundException(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_MESSAGE);
		});

		return mapper.toDto(userById);
	}

	public UserDto findByUsername(String username) {
		User userByUsername = userRepository.findByUsernameOptional(username).orElseThrow(() -> {
			LOG.errorf(AppMessages.USER_USERNAME_NOT_FOUND_EXCEPTION_DETAILED_MESSAGE, username);
			return new NotFoundException(AppMessages.USER_USERNAME_NOT_FOUND_EXCEPTION_MESSAGE);
		});

		return mapper.toDto(userByUsername);
	}

	public UserDto create(CreateUserDto createUserDto) {
		boolean existsUserWithUsername = userRepository.findByUsernameOptional(createUserDto.getUsername()).isPresent();
		if (Boolean.TRUE.equals(existsUserWithUsername)) {
			LOG.errorf(AppMessages.USERNAME_ALREADY_EXISTS_EXCEPTION_DETAILED_MESSAGE, createUserDto.getUsername());
			throw new BadRequestException(AppMessages.USERNAME_ALREADY_EXISTS_EXCEPTION_MESSAGE);
		}

		boolean existsUserWithEmail = userRepository.findByEmailOptional(createUserDto.getEmail()).isPresent();
		if (Boolean.TRUE.equals(existsUserWithEmail)) {
			LOG.errorf(AppMessages.EMAIL_ALREADY_EXISTS_EXCEPTION_DETAILED_MESSAGE, createUserDto.getUsername());
			throw new BadRequestException(AppMessages.EMAIL_ALREADY_EXISTS_EXCEPTION_MESSAGE);
		}

		ZonedDateTime now = ZonedDateTime.now(clock);

		User user = mapper.toEntity(createUserDto);
		user.setPassword(BcryptUtil.bcryptHash(user.getPassword()));
		user.setIsActive(Boolean.FALSE);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);

		userRepository.persist(user);
		return mapper.toDto(user);
	}

	public UserDto update(Long userId, UpdateUserDto updateUserDto) {
		User userById = userRepository.findByIdOptional(userId).orElseThrow(() -> {
			LOG.errorf(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_DETAILED_MESSAGE, userId);
			return new NotFoundException(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_MESSAGE);
		});

		if (updateUserDto.getName() != null)
			userById.setName(updateUserDto.getName());

		if (updateUserDto.getAge() != null)
			userById.setAge(updateUserDto.getAge());

		userById.setUpdatedAt(ZonedDateTime.now(clock));
		userRepository.persist(userById);
		return mapper.toDto(userById);
	}

	public void deleteById(Long userId) {
		User userById = userRepository.findByIdOptional(userId).orElseThrow(() -> {
			LOG.errorf(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_DETAILED_MESSAGE, userId);
			return new NotFoundException(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_MESSAGE);
		});

		userById.setIsActive(Boolean.FALSE);
		userById.setUpdatedAt(ZonedDateTime.now(clock));

		userRepository.persist(userById);
	}

	public void changePassword(Long userId, ChangePasswordDto changePasswordDto) {
		User userById = userRepository.findByIdOptional(userId).orElseThrow(() -> {
			LOG.errorf(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_DETAILED_MESSAGE, userId);
			return new NotFoundException(AppMessages.USER_ID_NOT_FOUND_EXCEPTION_MESSAGE);
		});

		if (!BcryptUtil.matches(changePasswordDto.getCurrentPassword(), userById.getPassword())) {
			LOG.errorf(AppMessages.UNAUTHORIZED_CREDENTIALS_EXCEPTION_DETAILED_MESSAGE,
					changePasswordDto.getCurrentPassword());
			throw new BadRequestException(AppMessages.UNAUTHORIZED_CREDENTIALS_EXCEPTION_MESSAGE);
		}

		if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
			LOG.errorf(AppMessages.PASSWORDS_NOT_MATCH_EXCEPTION_MESSAGE);
			throw new BadRequestException(AppMessages.PASSWORDS_NOT_MATCH_EXCEPTION_DETAILED_MESSAGE);
		}

		userById.setPassword(BcryptUtil.bcryptHash(changePasswordDto.getNewPassword()));
		userById.setUpdatedAt(ZonedDateTime.now(clock));

		userRepository.persist(userById);
	}

}
