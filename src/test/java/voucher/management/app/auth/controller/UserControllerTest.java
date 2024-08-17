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

	@BeforeEach
	void setUp() {
		testUser = new User("antonia@gmail.com", "Antonia", "Pwd@21212", RoleType.MERCHANT, true);

		mockUsers.add(DTOMapper.toUserDTO(testUser));
	}

	@AfterEach
	public void tearDown() {
		testUser = new User();

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



	}

}
