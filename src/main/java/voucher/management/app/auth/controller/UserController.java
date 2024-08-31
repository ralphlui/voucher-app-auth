package voucher.management.app.auth.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import voucher.management.app.auth.dto.ValidationResult;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.exception.UserNotFoundException;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.strategy.impl.UserValidationStrategy;
import voucher.management.app.auth.utility.DTOMapper;
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
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllActiveUsers(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API with page={}, size={}", page, size);

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findActiveUsers(pageable);
			logger.info("size" + resultMap.size());

			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> userDTOList = firstEntry.getValue();

			logger.info("totalRecord: " + totalRecord);
			logger.info("userDTO List: " + userDTOList);

			if (userDTOList.size() > 0) {
				return ResponseEntity.status(HttpStatus.OK).body(
						APIResponse.success(userDTOList, "Successfully get all active verified user.", totalRecord));

			} else {
				String message = "No Active User List.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.noList(userDTOList, message));
			}

		} catch (Exception e) {
			logger.error("Error, " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error("Error: " + e.getMessage()));
		}
	}

	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> createUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user create API...");
		String message;

		try {
			ValidationResult validationResult = userValidationStrategy.validateCreation(userRequest);
			if (validationResult.isValid()) {

				UserDTO userDTO = userService.createUser(userRequest);
				message = userRequest.getEmail() + " is created successfully";
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			} else {
				message = validationResult.getMessage();
				logger.error(message);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = "Error: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}

	}

	@PostMapping(value = "/login", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> loginUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user login API...");
		String message = "";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (!validationResult.isValid()) {
				logger.error("Login Valiation Error: " + validationResult.getMessage());
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}

			UserDTO userDTO = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
			message = userDTO.getEmail() + " login successfully";
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode)
					.body(APIResponse.error("Error: " + e.getMessage()));
		}
	}

	@PutMapping(value = "/verify/{verifyid}", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> verifyUser(@PathVariable("verifyid") String verifyid) {
		logger.info("Call user verify API with verifyToken={}", verifyid);
		verifyid = GeneralUtility.makeNotNull(verifyid);
		logger.info("Call user verify API with verifyToken={}", verifyid);

		String message = "";
		try {
			if (!verifyid.isEmpty()) {
				UserDTO verifiedUserDTO = userService.verifyUser(verifyid);
				message = "User successfully verified.";
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(verifiedUserDTO, message));

			} else {

				message = "Vefriy Id could not be blank.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			message = "Error: " + e.getMessage();
			logger.error(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}

	}

	@PutMapping(value = "/resetPassword", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> resetPassword(@RequestBody UserRequest resetPwdReq) {

		logger.info("Call user resetPassword API...");

		logger.info("Reset Password : " + resetPwdReq.getEmail());

		String message = "";
		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(resetPwdReq.getEmail());
			if (!validationResult.isValid()) {
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}

			User modifiedUser = userService.resetPassword(resetPwdReq);
			message = "Reset Password is completed.";
			logger.info(message + resetPwdReq.getEmail());
			logger.info(modifiedUser.getEmail());
			UserDTO userDTO = DTOMapper.toUserDTO(modifiedUser);
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error("Error, " + e.toString());
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(e.getMessage()));
		}

	}

	@PutMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> updateUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user update API...");
		String message;

		try {
			ValidationResult validationResult = userValidationStrategy.validateUpdating(userRequest);
			if (validationResult.isValid()) {

				UserDTO userDTO = userService.update(userRequest);
				message = "User updated successfully.";
				logger.info(message + userRequest.getEmail());
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

			} else {
				message = validationResult.getMessage();
				logger.error(message);
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = "Error: " + e.toString();
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(e.getMessage()));
		}
	}

	@GetMapping(value = "/active", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> checkSpecificActiveUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user active API...");
		logger.info("email" + userRequest.getEmail());
		String message = "";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (!validationResult.isValid()) {
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}

			User user = userService.checkSpecificActiveUser(userRequest.getEmail());
			UserDTO userDTO = DTOMapper.toUserDTO(user);
			message = user.getEmail() + " is Active";
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.BAD_REQUEST
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error("Error: " + e.getMessage()));
		}
	}

	@GetMapping(value = "/preferences/{preference}", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllUsersByPreferences(
			@PathVariable("preference") String preference, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API By Preferences with page={}, size={}", page, size);

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findUsersByPreferences(preference,pageable);
			
			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> userDTOList = firstEntry.getValue();
			
			logger.info("totalRecord: " + totalRecord);
			logger.info("userDTO List: " + userDTOList);

			if (userDTOList.size() > 0) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(APIResponse.success(userDTOList, "Successfully get all active user by preferences.", totalRecord));

			} else {
				String message = "No user list by this preferences.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.noList(userDTOList, message));
			}

		} catch (Exception e) {
			logger.error("Error, " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error("Error: " + e.getMessage()));
		}
	}
	
	@DeleteMapping(value = "/preferences", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> deletePreferenceByUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user Delete Preferences API...");
		String message;

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (validationResult.isValid()) {

				UserDTO userDTO = userService.deletePreferencesByUser(userRequest);
				message = "Preferences deleted successfully.";
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			} else {
				message = validationResult.getMessage();
				logger.error(message);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = e.getMessage();
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.BAD_REQUEST;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}

	}

}
