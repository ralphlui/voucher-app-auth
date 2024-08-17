package voucher.management.app.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;
import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.enums.RoleType;
import voucher.management.app.auth.repository.UserRepository;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.utility.EncryptionUtils;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserServiceTest {
	
	private static List<User> mockUsers = new ArrayList<>();
	
	@Autowired
	private UserService userService;
	
	@MockBean
	private UserRepository userRepository;
	
	@MockBean
	private PasswordEncoder passwordEncoder;
	
	@MockBean
	private EncryptionUtils encryptionUtils;

	
	private static User user = new User("admin12345@gmail.com", "Admin", "Pwd@123", RoleType.ADMIN, true);

	@BeforeEach
	void setUp() {
		mockUsers.add(user);

	}

	@Test
	void getAllActiveUsers() {

		long totalRecord = 0;
		List<UserDTO> userDTOList = new ArrayList<UserDTO>();
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> mockUserPages = new PageImpl<>(mockUsers, pageable, mockUsers.size());

		Mockito.when(userRepository.findByIsActiveTrue(pageable)).thenReturn(mockUserPages);
		Map<Long, List<UserDTO>> userPages = userService.findActiveUsers(pageable);

		for (Map.Entry<Long, List<UserDTO>> entry : userPages.entrySet()) {
			totalRecord = entry.getKey();
			userDTOList = entry.getValue();

		}
		assertEquals(mockUsers.size(), userDTOList.size());
		assertEquals(mockUsers.get(0).getEmail(), userDTOList.get(0).getEmail());

	}
	

	@Test
	void createUser() {

		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
		Mockito.when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		User createdUser = userService.createUser(user);
		assertThat(createdUser).isNotNull();
		assertThat(createdUser.getEmail().equals("admin12345@gmail.com")).isTrue();

	}
	
	@Test
    public void testValidateUserLogin_Successful() {
        
        Mockito.when(userRepository.findByEmailAndStatus(user.getEmail(), true, true)).thenReturn(user);
        Mockito.when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(true);

        User result = userService.loginUser(user.getEmail(), user.getPassword());

        assertEquals(user, result);
    }
	

	@Test
	public void verifyUser() throws Exception {
		String decodedVerificationCode = "7f03a9a9-d7a5-4742-bc85-68d52b2bee45";
		String verificationCode = encryptionUtils.encrypt(decodedVerificationCode);
		user.setVerified(false);

		Mockito.when(encryptionUtils.decrypt(verificationCode)).thenReturn(decodedVerificationCode);
		Mockito.when(userRepository.findByVerificationCode(decodedVerificationCode, false, true)).thenReturn(user);
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

		UserDTO userDTO = userService.verifyUser(verificationCode);

		assertThat(user.isVerified()).isTrue();
		assertThat(userDTO).isNotNull();
	}
	
	@Test
	void updateUser() {

		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
		Mockito.when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		user.setActive(true);
		user.setUsername("test12");
		user.setUpdatedDate(LocalDateTime.now());

		User updatedUser = userService.update(user);
		assertThat(updatedUser).isNotNull();
		assertThat(updatedUser.getEmail().equals("admin12345@gmail.com")).isTrue();

	}

}
