package voucher.management.app.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.dto.UserRequest;
import voucher.management.app.auth.entity.User;

public interface IUserService {
	Map<Long, List<UserDTO>> findActiveUsers(Pageable pageable);
	
	 User createUser(User user);
	 
	 User findByEmail(String email);
	 
	 User loginUser(String email, String password);
	 
	 User verifyUser(String verificationCode) throws Exception;
	 
	 User findByEmailAndStatus(String email, boolean isActive, boolean isVerified);
	 
	 User update(User user);
	 
	 Map<Long, List<UserDTO>> findUsersByPreferences(String preferences, Pageable pageable);
	 
	 User resetPassword(UserRequest userRequest);
	 
	 User checkSpecificActiveUser(String email);
}
