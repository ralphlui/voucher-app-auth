package voucher.management.app.auth.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import voucher.management.app.auth.dto.APIResponse;
import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.dto.UserRequest;
import voucher.management.app.auth.dto.UserResponse;
import voucher.management.app.auth.dto.ValidationResult;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.strategy.impl.UserValidationStrategy;
import voucher.management.app.auth.utility.GeneralUtility;

import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/users")
public class UserController {
	
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UserValidationStrategy userValidationStrategy;


	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API with page={}, size={}", page, size);
		long totalRecord = 0;

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findActiveUsers(pageable);

			if (resultMap.size() == 0) {
				String message = "User not found.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));
			}

			List<UserDTO> userDTOList = new ArrayList<UserDTO>();

			for (Map.Entry<Long, List<UserDTO>> entry : resultMap.entrySet()) {
				totalRecord = entry.getKey();
				userDTOList = entry.getValue();

				logger.info("totalRecord: " + totalRecord);
				logger.info("userDTO List: " + userDTOList);

			}

			if (userDTOList.size() > 0) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(APIResponse.success(userDTOList, "Successfully get all active user.", totalRecord));

			} else {
				String message = "User not found.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));
			}

		} catch (Exception e) {
			logger.error("Error, " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error("Error: " + e.getMessage()));
		}
	}
	
	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserResponse>> createUser(@RequestBody User user) {
		logger.info("Call user create API...");
		String message;

		try {
			ValidationResult validationResult = userValidationStrategy.validateCreation(user);
			if (validationResult.isValid()) {
	
				User createdUser  = userService.createUser(user);
				
				if (createdUser != null && !createdUser.getEmail().isEmpty()) {
					UserResponse userResp = new UserResponse();
					userResp.setEmail(user.getEmail());
					userResp.setUsername(user.getUsername());
					userResp.setRole(user.getRole());
					message = user.getEmail() + " is created successfully";
					return ResponseEntity.status(HttpStatus.OK)
							.body(APIResponse.success(userResp, message));
				} else {
					message = "User registraiton is not successful.";
					logger.error(message);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
				}
			} else {
				message = validationResult.getMessage();
				logger.error(message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = "Error: " + e.toString();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message));
		}

	}
	
	@PostMapping(value = "/login", produces = "application/json")
	public ResponseEntity<APIResponse<UserResponse>> loginUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user login API...");
		String message = "";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (!validationResult.isValid()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(validationResult.getMessage()));
			}

			User user = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
			if (user != null) {
				UserResponse userResp = new UserResponse();
				userResp.setEmail(user.getEmail());
				userResp.setUsername(user.getUsername());
				userResp.setRole(user.getRole());
				message = user.getEmail() + " login successfully";
				return ResponseEntity.status(HttpStatus.OK)
						.body(APIResponse.success(userResp, message));
			} else {
				message = "Invalid credentials.";
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error("Error: " + e.toString()));
		}
	}
	
	@PutMapping(value = "/verify/{verifyid}", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> verifyUser(@PathVariable("verifyid") String verifyid) {
		verifyid = GeneralUtility.makeNotNull(verifyid);
		logger.info("Call user verify API with verifyToken={}", verifyid);

		String message = "";
		try {
			if (!verifyid.isEmpty()) {
				UserDTO userDTO = userService.verifyUser(verifyid);
				if (userDTO != null) {
					message = "User successfully verified.";

					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

				} else {

					message = "Vefriy user failed: Verfiy Id is invalid or already verified.";
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
				}
			} else {

				message = "Vefriy Id could not be blank.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			message = "Error: " + e.toString();
			logger.error(message);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}

	}

}
