package com.bok.chat.api.service;

import com.bok.chat.api.dto.LoginRequest;
import com.bok.chat.api.dto.LoginResponse;
import com.bok.chat.api.dto.RegisterRequest;
import com.bok.chat.entity.User;
import com.bok.chat.repository.UserRepository;
import com.bok.chat.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.bok.chat.support.TestFixtures.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("UserService")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공 시 userId를 반환한다")
    void register_shouldReturnUserId() {
        RegisterRequest request = new RegisterRequest("testuser", "password");
        User savedUser = createUser(1L, "testuser");

        given(userRepository.existsByUsername("testuser")).willReturn(false);
        given(passwordEncoder.encode("password")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        Long userId = userService.register(request);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("중복된 username으로 가입하면 예외가 발생한다")
    void register_duplicateUsername_shouldThrow() {
        RegisterRequest request = new RegisterRequest("testuser", "password");

        given(userRepository.existsByUsername("testuser")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 username");
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 사용자 정보를 반환한다")
    void login_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest("testuser", "password");
        User user = createUser(1L, "testuser");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", user.getPassword())).willReturn(true);
        given(jwtProvider.generateToken(1L, "testuser")).willReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void login_wrongPassword_shouldThrow() {
        LoginRequest request = new LoginRequest("testuser", "wrong");
        User user = createUser(1L, "testuser");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인하면 예외가 발생한다")
    void login_nonExistentUser_shouldThrow() {
        LoginRequest request = new LoginRequest("nouser", "password");

        given(userRepository.findByUsername("nouser")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");
    }
}
