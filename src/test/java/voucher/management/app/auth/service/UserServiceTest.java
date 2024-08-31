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
import voucher.management.app.auth.dto.UserRequest;
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

	private static UserRequest userRequest = new UserRequest("useradmin@gmail.com", "Pwd@123", "UserAdmin", RoleType.ADMIN, true, new ArrayList<String>());

	@BeforeEach
	void setUp() {
		user.setPreferences("food");
		mockUsers.add(user);

	}

	@Test
	void getAllActiveUsers() {

		List<UserDTO> userDTOList = new ArrayList<UserDTO>();
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> mockUserPages = new PageImpl<>(mockUsers, pageable, mockUsers.size());

		Mockito.when(userRepository.findByIsActiveTrue(pageable)).thenReturn(mockUserPages);
		Map<Long, List<UserDTO>> userPages = userService.findActiveUsers(pageable);

		for (Map.Entry<Long, List<UserDTO>> entry : userPages.entrySet()) {
			userDTOList = entry.getValue();

		}
		assertEquals(mockUsers.size(), userDTOList.size());
		assertEquals(mockUsers.get(0).getEmail(), userDTOList.get(0).getEmail());

	}
	

	@Test
	void createUser() throws Exception {

		user.setEmail(userRequest.getEmail());
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
		Mockito.when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		UserDTO createdUser = userService.createUser(userRequest);
		assertThat(createdUser).isNotNull();
		assertThat(createdUser.getEmail().equals("useradmin@gmail.com")).isTrue();

	}
	
	@Test
    public void testValidateUserLogin_Successful() {
        
        Mockito.when(userRepository.findByEmailAndStatus(user.getEmail(), true, true)).thenReturn(user);
        Mockito.when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(true);

        UserDTO result = userService.loginUser(user.getEmail(), user.getPassword());

        assertEquals(user.getEmail(), result.getEmail());
    }
	

	@Test
	public void verifyUser() throws Exception {
		String decodedVerificationCode = "7f03a9a9-d7a5-4742-bc85-68d52b2bee45";
		String verificationCode = encryptionUtils.encrypt(decodedVerificationCode);
		user.setVerified(false);

		Mockito.when(encryptionUtils.decrypt(verificationCode)).thenReturn(decodedVerificationCode);
		Mockito.when(userRepository.findByVerificationCode(decodedVerificationCode, false, true)).thenReturn(user);
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

		User verifiedUser = userService.verifyUser(verificationCode);

		assertThat(user.isVerified()).isTrue();
		assertThat(verifiedUser).isNotNull();
	}
	
	@Test
	void updateUser() throws Exception {

		user.setActive(userRequest.getActive());
		user.setUsername(userRequest.getUsername());
		user.setUpdatedDate(LocalDateTime.now());
		Mockito.when(userService.findByEmail(userRequest.getEmail())).thenReturn(user);

		Mockito.when(userRepository.save(user)).thenReturn(user);
		Mockito.when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserDTO updatedUser = userService.update(userRequest);
		assertThat(updatedUser.getUsername().equals("UserAdmin")).isTrue();

	}
	
	@Test
    public void testFindByEmailAndStatus() {
        
        Mockito.when(userRepository.findByEmailAndStatus(user.getEmail(), true, true)).thenReturn(user);

        User result = userService.findByEmailAndStatus(user.getEmail(), true, true);

        assertEquals(user, result);
    }
	
	@Test
	void getAllActiveUsersByPreferences() {

		List<UserDTO> userDTOList = new ArrayList<UserDTO>();
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> mockUserPages = new PageImpl<>(mockUsers, pageable, mockUsers.size());

		Mockito.when(userRepository.findByPreferences("clothing", true, pageable)).thenReturn(mockUserPages);
		Map<Long, List<UserDTO>> userPages = userService.findUsersByPreferences("clothing", pageable);

		for (Map.Entry<Long, List<UserDTO>> entry : userPages.entrySet()) {
			userDTOList = entry.getValue();

		}
		assertEquals(mockUsers.size(), userDTOList.size());
		assertEquals(mockUsers.get(0).getEmail(), userDTOList.get(0).getEmail());
		
		
		Map<Long, List<UserDTO>> userList = userService.findUsersByPreferences("shoes", pageable);
		assertEquals(userList, null);


	}
	
	@Test
	void resetPassword() throws Exception {

		Mockito.when(userRepository.findByEmailAndStatus(user.getEmail(), true, true)).thenReturn(user);
		Mockito.when(userRepository.save(user)).thenReturn(user);
     

    	UserRequest userRequest = new UserRequest(user.getEmail(), "Pwd@21212");
		User updatedUser = userService.resetPassword(userRequest);
		assertThat(updatedUser.getEmail().equals("admin12345@gmail.com")).isTrue();

	}
	
	@Test
	void checkSpecificActiveUser() throws Exception {

		Mockito.when(userRepository.findByEmailAndStatus(user.getEmail(), true, true)).thenReturn(user);
     
		User activeUser = userService.checkSpecificActiveUser(user.getEmail());
		assertThat(activeUser.getEmail().equals(user.getEmail())).isTrue();
		
	}
	
	@Test
	void deletePreferencesByUser() throws Exception {

		ArrayList<String> deletedPreferenceList = new ArrayList<String>();
		deletedPreferenceList.add("food");
		userRequest.setPreferences(deletedPreferenceList);
		user.setEmail(userRequest.getEmail());
		Mockito.when(userService.findByEmail(user.getEmail())).thenReturn(user);
		Mockito.when(userRepository.save(user)).thenReturn(user);
     
	    UserDTO updateUser = userService.deletePreferencesByUser(userRequest);
		assertThat(updateUser.getEmail().equals("useradmin@gmail.com")).isTrue();
		
	}

}
