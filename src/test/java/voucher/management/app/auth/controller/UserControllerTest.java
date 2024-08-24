package voucher.management.app.auth.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.dto.UserRequest;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.enums.RoleType;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.utility.DTOMapper;
import voucher.management.app.auth.utility.EncryptionUtils;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UserControllerTest {
	

	@MockBean
	private UserService userService;

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private EncryptionUtils encryptionUtils;

	
	User testUser;

	private static List<UserDTO> mockUsers = new ArrayList<>();
	User errorUser = new User("error@gmail.com", "Error", "Pwd@21212", RoleType.MERCHANT, true);

	@BeforeEach
	void setUp() {
		testUser = new User("antonia@gmail.com", "Antonia", "Pwd@21212", RoleType.MERCHANT, true);

		mockUsers.add(DTOMapper.toUserDTO(testUser));
	}

	@AfterEach
	public void tearDown() {
		testUser = new User();
		errorUser = new User();

	}

	
	@Test
	public void testGetAllUser() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());
		Map<Long, List<UserDTO>> mockUserMap = new HashMap<>();
		mockUserMap.put(0L, mockUsers);

		Mockito.when(userService.findActiveUsers(pageable)).thenReturn(mockUserMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/users").param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully get all active user.")).andDo(print());
	}
	
	@Test
	public void testUserLogin() throws Exception {
		testUser.setVerified(true);
		UserRequest userRequest = new UserRequest(testUser.getEmail(), "Pwd@21212");
		Mockito.when(userService.findByEmail(userRequest.getEmail())).thenReturn(testUser);

		Mockito.when(userService.loginUser(userRequest.getEmail(), userRequest.getPassword()))
				.thenReturn(testUser);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(userRequest))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value(testUser.getEmail() + " login successfully"))
				.andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
				.andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
				.andExpect(jsonPath("$.data.role").value(testUser.getRole().toString())).andDo(print());
		
		UserRequest invalidCredentialsUserRequest = new UserRequest(testUser.getEmail(), "12345678");
		Mockito.when(userService.findByEmail(userRequest.getEmail())).thenReturn(testUser);

		Mockito.when(userService.loginUser(userRequest.getEmail(), userRequest.getPassword()))
				.thenReturn(testUser);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidCredentialsUserRequest))).andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Invalid credentials."))
				.andExpect(jsonPath("$.success").value(false))
				.andDo(print());
		
		
		
		UserRequest userNotFoundRequest = new UserRequest(errorUser.getEmail(), "Pwd@21212");
		
		mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(userNotFoundRequest))).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("User account not found."))
				.andExpect(jsonPath("$.success").value(false))
				.andDo(print());
	}
	


	@Test
	public void testVerifyUser() throws Exception {

		String decodedVerificationCode = "7f03a9a9-d7a5-4742-bc85-68d52b2bee45";
		String verificationCode = encryptionUtils.encrypt(decodedVerificationCode);
		testUser.setVerified(false);
		testUser.setActive(true);
		testUser.setVerificationCode(decodedVerificationCode);
		Mockito.when(userService.findByEmailAndStatus(testUser.getEmail(), true, true)).thenReturn(testUser);

		Mockito.when(userService.verifyUser(verificationCode)).thenReturn(DTOMapper.toUserDTO(testUser));

		mockMvc.perform(MockMvcRequestBuilders.put("/api/users/verify/{verifyid}", verificationCode)
				).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());
		
		
		String errorDecodedVerificationCode = "7f03a9a9-d7a5-4742-bc85-68d52b2bee46";
		String errorVerificationCode = encryptionUtils.encrypt(errorDecodedVerificationCode);
		
		mockMvc.perform(MockMvcRequestBuilders.put("/api/users/verify/{verifyid}", errorVerificationCode)
				).andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Vefriy user failed: Verfiy Id is invalid or already verified.")).
				andDo(print());
		
		
		String errorDecodedVerificationCode2 = "";
		String errorVerificationCode2 = encryptionUtils.encrypt(errorDecodedVerificationCode2);
		mockMvc.perform(MockMvcRequestBuilders.put("/api/users/verify/{verifyid}", errorVerificationCode2)
				).andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false)).
				andDo(print());
		
		
	}
	

	@Test
	public void testCreateUser() throws Exception {
		Mockito.when(userService.createUser(Mockito.any(User.class)))
	   .thenReturn(testUser);
		

		mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testUser)))
		        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
				.andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
				.andExpect(jsonPath("$.data.role").value(testUser.getRole().toString())).andDo(print());
		
		User errorUser = new User("", "Error", "Pwd@21212", RoleType.MERCHANT, true);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(errorUser)))
		        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
		        .andExpect(jsonPath("$.success").value(false))
		        .andExpect(jsonPath("$.message").value("Email cannot be empty."))
		        .andDo(print());
		
    }
	
	
	@Test
	public void testResetPassword() throws Exception {
		testUser.setVerified(true);
		UserRequest userRequest = new UserRequest(testUser.getEmail(), "Pwd@21212");
		Mockito.when(userService.findByEmail(userRequest.getEmail())).thenReturn(testUser);
		Mockito.when(userService.update(testUser)).thenReturn(testUser);
		Mockito.when(userService.findByEmailAndStatus(testUser.getEmail(),true,true)).thenReturn(testUser);
		

		mockMvc.perform(MockMvcRequestBuilders.put("/api/users/resetPassword").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(userRequest))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Reset Password Completed.")).andDo(print());

	}
	

	@Test
	public void testUpdatedUser() throws Exception {
		Mockito.when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);

		Mockito.when(userService.update(testUser)).thenReturn(testUser);
		

		mockMvc.perform(MockMvcRequestBuilders.put("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testUser)))
		        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
				.andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
				.andExpect(jsonPath("$.data.role").value(testUser.getRole().toString())).andDo(print());
		
		mockMvc.perform(MockMvcRequestBuilders.put("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(errorUser)))
		        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("User not found."))
				.andDo(print());
    
	}
	
	@Test
	public void testActiveUser() throws Exception {
		Mockito.when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
		testUser.setVerified(true);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/users/active").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testUser)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
				.andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
				.andExpect(jsonPath("$.data.active").value(testUser.isActive())).andDo(print());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/users/active").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(errorUser)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false)).andDo(print());
	}

	@Test
	public void testGetAllUsersByPreferences() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());
		Map<Long, List<UserDTO>> mockUserMap = new HashMap<>();
		mockUserMap.put(0L, mockUsers);

		Mockito.when(userService.findUsersByPreferences("clothing", pageable)).thenReturn(mockUserMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/users/preferences/{preference}", "clothing").param("page", "0")
				.param("size", "10").contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully get all active user by preferences."))
				.andDo(print());

		Mockito.when(userService.findUsersByPreferences("shoes", pageable)).thenReturn(mockUserMap);
		mockMvc.perform(MockMvcRequestBuilders.get("/api/users/preferences/{preference}", "food").param("page", "0")
				.param("size", "10").contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false)).andDo(print());

	}
}
