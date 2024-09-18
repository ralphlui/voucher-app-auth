package voucher.management.app.auth.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import voucher.management.app.auth.enums.AuditLogInvalidUser;
import voucher.management.app.auth.enums.AuditLogResponseStatus;
import voucher.management.app.auth.exception.UserNotFoundException;
import voucher.management.app.auth.service.AuditLogService;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.strategy.impl.UserValidationStrategy;
import voucher.management.app.auth.utility.GeneralUtility;

import org.springframework.data.domain.Sort;

@RestController
@EnableAsync
@RequestMapping("/api/users")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private UserValidationStrategy userValidationStrategy;
	
	@Autowired
	private AuditLogService auditLogService;
	
	private String auditLogResponseSuccess = AuditLogResponseStatus.SUCCESS.toString();
	private String auditLogResponseFailure = AuditLogResponseStatus.FAILED.toString();
	private String auditLogInvalidUserId = AuditLogInvalidUser.InvalidUserID.toString();
	private String auditLogInvalidUserName = AuditLogInvalidUser.InvalidUserName.toString();
	

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllActiveUsers(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API with page={}, size={}", page, size);

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findActiveUsers(pageable);
			logger.info("all active user list size " + resultMap.size());

			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> userDTOList = firstEntry.getValue();

			logger.info("totalRecord: " + totalRecord);
			logger.info("userDTO List: " + userDTOList);

			if (userDTOList.size() > 0) {
				logger.info("Successfully get all active verified user.");
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
				logger.info(message);
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
		String activityType = "Authentication-Login";
		String apiEndPoint = "api/users/login";
		String httpMethod = HttpMethod.GET.name();
		String activityDesc = "User failed to login due to ";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			
			if (!validationResult.isValid()) {
				
				logger.error("Login Validation Error: " + validationResult.getMessage());
				message = validationResult.getMessage();
				activityDesc = activityDesc.concat(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(validationResult.getStatus().value()), validationResult.getUserId(), validationResult.getUserName(), activityType, activityDesc , apiEndPoint, auditLogResponseFailure, httpMethod, message);
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(message));
			}

			UserDTO userDTO = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
			message = userDTO.getEmail() + " login successfully";
			logger.info(message);
			auditLogService.sendAuditLogToSqs(Integer.toString(HttpStatus.OK.value()), userDTO.getUserID(), userDTO.getUsername(), activityType, message, apiEndPoint, auditLogResponseSuccess, httpMethod, "");
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			
		} catch (Exception e) {
			
			logger.error("Error: " + e.toString());
			message = e.getMessage();
			activityDesc = activityDesc.concat(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
			auditLogService.sendAuditLogToSqs(Integer.toString(htpStatuscode.value()), auditLogInvalidUserId, auditLogInvalidUserName, activityType, activityDesc , apiEndPoint, auditLogResponseFailure, httpMethod, message);
			return ResponseEntity.status(htpStatuscode)
					.body(APIResponse.error("Error: " + message));
		}
	}

	@PatchMapping(value = "/verify/{verifyid}", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> verifyUser(@PathVariable("verifyid") String verifyid) {
		logger.info("Call user verify API with verifyToken={}", verifyid);
		verifyid = GeneralUtility.makeNotNull(verifyid);
		logger.info("Call user verify API with verifyToken={}", verifyid);

		String message = "";
		try {
			if (!verifyid.isEmpty()) {
				UserDTO verifiedUserDTO = userService.verifyUser(verifyid);
				message = "User successfully verified.";
				logger.info(message);
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

	@PatchMapping(value = "/{id}/resetPassword", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> resetPassword(@PathVariable("id") String id, @RequestBody UserRequest resetPwdReq) {

		logger.info("Call user resetPassword API...");

		logger.info("Reset Password : " + resetPwdReq.getEmail());

		String message = "";
		try {
			ValidationResult validationResult = userValidationStrategy.validateObjectByUserId(id);
			if (!validationResult.isValid()) {
				logger.error("Reset passwrod validation is not successful");
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}

			UserDTO userDTO = userService.resetPassword(id, resetPwdReq.getPassword());
			message = "Reset Password is completed.";
			logger.info(message + resetPwdReq.getEmail());
			logger.info(userDTO.getEmail());
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error("Error, " + e.toString());
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(e.getMessage()));
		}

	}

	@PutMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> updateUser(@PathVariable("id") String id, @RequestBody UserRequest userRequest) {
		logger.info("Call user update API...");
		String message;
		String activityType = "Authentication-UpdateUser";
		String apiEndPoint = "api/users/{id}";
		String httpMethod = HttpMethod.PUT.name();
		String activityDesc = "Update User failed due to ";

		try {
			ValidationResult validationResult = userValidationStrategy.validateUpdating(id);
			if (validationResult.isValid()) {

				userRequest.setUserId(id);
				UserDTO userDTO = userService.update(userRequest);
				message = "User updated successfully.";
				logger.info(message + userRequest.getEmail());
				auditLogService.sendAuditLogToSqs(Integer.toString(HttpStatus.OK.value()), userDTO.getUserID(), userDTO.getUsername(), activityType, message, apiEndPoint, auditLogResponseSuccess, httpMethod, "");
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

			} else {
				message = validationResult.getMessage();
				logger.error(message);
				activityDesc = activityDesc.concat(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(validationResult.getStatus().value()), validationResult.getUserId(), validationResult.getUserName(), activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(validationResult.getMessage()));
			}
		} catch (Exception e) {
			
			logger.error("Error: " + e.toString());
			message = "Error: " + e.toString();
			activityDesc = activityDesc.concat(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED
					: HttpStatus.INTERNAL_SERVER_ERROR;
			auditLogService.sendAuditLogToSqs(Integer.toString(htpStatuscode.value()), auditLogInvalidUserId, auditLogInvalidUserName, activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(e.getMessage()));
		}
	}

	@GetMapping(value = "/{id}/active", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> checkSpecificActiveUser(@PathVariable("id") String id) {
		logger.info("Call user active API...");
		logger.info("User ID" + id);
		String message = "";
		String activityType = "Authentication-RetrieveActiveUserByUserId";
		String apiEndPoint = "api/users/{id}/active";
		String httpMethod = HttpMethod.GET.name();
		String activityDesc = "Retrieving active user by id failed due to ";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObjectByUserId(id);
			if (!validationResult.isValid()) {
				logger.error("Active user validation is not successful");
				message = validationResult.getMessage();
				activityDesc = activityDesc.concat(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(validationResult.getStatus().value()), validationResult.getUserId(), validationResult.getUserName(), activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
				return ResponseEntity.status(validationResult.getStatus())
						.body(APIResponse.error(message));
			}

			UserDTO userDTO = userService.checkSpecificActiveUser(id);
			message = userDTO.getEmail() + " is Active";
			logger.info(message);
			auditLogService.sendAuditLogToSqs(Integer.toString(HttpStatus.OK.value()), userDTO.getUserID(), userDTO.getUsername(), activityType, message, apiEndPoint, auditLogResponseSuccess, httpMethod, "");
        	return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = e.getMessage();
			activityDesc = activityDesc.concat(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.BAD_REQUEST
					: HttpStatus.INTERNAL_SERVER_ERROR;
			auditLogService.sendAuditLogToSqs(Integer.toString(htpStatuscode.value()), auditLogInvalidUserId, auditLogInvalidUserName, activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}
	}

	@GetMapping(value = "/preferences/{name}", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllUsersByPreferences(
			@PathVariable("name") String name, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API By Preferences with page={}, size={}", page, size);

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findUsersByPreferences(name, pageable);
			
			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> userDTOList = firstEntry.getValue();
			
			logger.info("totalRecord: " + totalRecord);
			logger.info("userDTO List: " + userDTOList);

			if (userDTOList.size() > 0) {
				logger.info("Successfully get all active user by preferences.");
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
	
	@DeleteMapping(value = "/{id}/preferences", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> deletePreferenceByUser(@PathVariable("id") String id, @RequestBody UserRequest userRequest) {
		logger.info("Call user Delete Preferences API...");
		String message;
		String activityType = "Authentication-DeleteUserPreferenceByUserId";
		String apiEndPoint = "api/users/{id}/preferences";
		String httpMethod = HttpMethod.DELETE.name();
		String activityDesc = "Delete user preference by preference is failed due to ";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObjectByUserId(id);
			if (validationResult.isValid()) {

				UserDTO userDTO = userService.deletePreferencesByUser(id, userRequest.getPreferences());
				message = "Preferences deleted successfully.";
				logger.info(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(HttpStatus.OK.value()), userDTO.getUserID(), userDTO.getUsername(), activityType, message, apiEndPoint, auditLogResponseSuccess, httpMethod, "");
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			} else {
				message = validationResult.getMessage();
				logger.error(message);
				activityDesc = activityDesc.concat(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(validationResult.getStatus().value()), validationResult.getUserId(), validationResult.getUserName(), activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = e.getMessage();
			activityDesc = activityDesc.concat(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.BAD_REQUEST;
			auditLogService.sendAuditLogToSqs(Integer.toString(htpStatuscode.value()), auditLogInvalidUserId, auditLogInvalidUserName, activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}

	}
	
	
	@PatchMapping(value = "/{id}/preferences", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> updatereferenceByUser(@PathVariable("id") String id, @RequestBody UserRequest userRequest) {
		logger.info("Call user update Preferences API...");
		String message;
		String activityType = "Authentication-UpdateUserPreferenceByUserId";
		String apiEndPoint = "api/users/{id}/preferences";
		String httpMethod = HttpMethod.PATCH.name();
		String activityDesc = "Update user preference by preference is failed due to ";

		try {
			ValidationResult validationResult = userValidationStrategy.validateObjectByUserId(id);
			if (validationResult.isValid()) {

				UserDTO userDTO = userService.updatePreferencesByUser(id, userRequest.getPreferences());
				message = "Preferences are updated successfully.";
				logger.info(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(HttpStatus.OK.value()), userDTO.getUserID(), userDTO.getUsername(), activityType, message, apiEndPoint, auditLogResponseSuccess, httpMethod, "");
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));
			} else {
				message = validationResult.getMessage();
				logger.error(message);
				activityDesc = activityDesc.concat(message);
				auditLogService.sendAuditLogToSqs(Integer.toString(validationResult.getStatus().value()), validationResult.getUserId(), validationResult.getUserName(), activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.toString());
			message = e.getMessage();
			activityDesc = activityDesc.concat(message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.BAD_REQUEST;
			auditLogService.sendAuditLogToSqs(Integer.toString(htpStatuscode.value()), auditLogInvalidUserId, auditLogInvalidUserName, activityType, activityDesc, apiEndPoint, auditLogResponseFailure, httpMethod, message);	
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}

	}

}
