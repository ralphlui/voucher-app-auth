package voucher.management.app.auth.strategy.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import voucher.management.app.auth.dto.ValidationResult;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.service.impl.UserService;
import voucher.management.app.auth.strategy.IAPIHelperValidationStrategy;

@Service
public class UserValidationStrategy implements IAPIHelperValidationStrategy<User> {

	@Autowired
	private UserService userService;

	@Override
	public ValidationResult validateCreation(User user) {
		ValidationResult validationResult = new ValidationResult();

		if (user.getEmail() == null || user.getEmail().isEmpty()) {
			validationResult.setMessage("Email cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		User dbUser = userService.findByEmail(user.getEmail());
		if (dbUser != null) {
			validationResult.setMessage("User already exists.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}

}
