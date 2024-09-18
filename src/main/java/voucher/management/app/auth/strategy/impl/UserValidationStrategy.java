package voucher.management.app.auth.strategy.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import voucher.management.app.auth.dto.UserRequest;
import voucher.management.app.auth.dto.ValidationResult;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.enums.AuditLogInvalidUser;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.strategy.IAPIHelperValidationStrategy;

@Service
public class UserValidationStrategy implements IAPIHelperValidationStrategy<UserRequest> {

	@Autowired
	private UserService userService;
	
	private String auditLogInvalidUserId = AuditLogInvalidUser.InvalidUserID.toString();
	private String auditLogInvalidUserName = AuditLogInvalidUser.InvalidUserName.toString();
	

	@Override
	public ValidationResult validateCreation(UserRequest userRequest) {
		ValidationResult validationResult = new ValidationResult();

		if (userRequest.getEmail() == null || userRequest.getEmail().isEmpty()) {
			validationResult.setMessage("Email cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		User dbUser = userService.findByEmail(userRequest.getEmail());
		if (dbUser != null) {
			validationResult.setMessage("User already exixts.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}
	
	@Override
	public ValidationResult validateObject(String email) {
	    ValidationResult validationResult = new ValidationResult();
	    
	    User user = userService.findByEmail(email);

	    if (user == null) {
	        validationResult.setMessage("User account not found.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        validationResult.setUserId(auditLogInvalidUserId);
	        validationResult.setUserName(auditLogInvalidUserName);
	        return validationResult;
	    }

	   
	    validationResult.setUserId(user.getUserId());
	    validationResult.setUserName(user.getUsername());

	   
	    if (!user.isActive()) {
	        validationResult.setMessage("User account is deleted.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        return validationResult;
	    }

	   
	    if (!user.isVerified()) {
	        validationResult.setMessage("User has not been verified.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        return validationResult;
	    }

	  
	    validationResult.setValid(true);
	    return validationResult;
	}
	
	
	@Override
	public ValidationResult validateUpdating(String userId) {
		ValidationResult validationResult = new ValidationResult();

		if (userId == null || userId.isEmpty()) {
			validationResult.setMessage("User ID cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(auditLogInvalidUserId);
			validationResult.setUserName(auditLogInvalidUserName);
			return validationResult;
		}
		
		ValidationResult validationObjResult = validateObjectByUserId(userId);
		if (!validationObjResult.isValid()) {
			return validationObjResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}

	@Override
	public ValidationResult validateObjectByUserId(String userId) {
	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setUserId(userId); 

	    User user = userService.findByUserId(userId);
	    
	    if (user == null) {
	        validationResult.setMessage("User account not found.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        validationResult.setUserName(auditLogInvalidUserName);
	        return validationResult; 
	    }
	    
	    validationResult.setUserName(user.getUsername());
	    
	    if (!user.isActive()) {
	        validationResult.setMessage("User account is deleted.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        return validationResult;
	    }
	    
	    if (!user.isVerified()) {
	        validationResult.setMessage("Please verify the account first.");
	        validationResult.setStatus(HttpStatus.UNAUTHORIZED);
	        validationResult.setValid(false);
	        return validationResult;
	    }

	    validationResult.setValid(true);
	    return validationResult;
	}

}
