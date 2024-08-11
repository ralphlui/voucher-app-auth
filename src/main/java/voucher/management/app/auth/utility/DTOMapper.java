package voucher.management.app.auth.utility;

import org.springframework.stereotype.Component;

import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.entity.User;

@Component
public class DTOMapper {
	

	public static UserDTO toUserDTO(User user) {
		UserDTO userDTO = new UserDTO();
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setRole(user.getRole());
		userDTO.setActive(user.isActive());
		userDTO.setLastLoginDate(user.getLastLoginDate());
		userDTO.setCreatedDate(user.getCreatedDate());
		userDTO.setUpdatedDate(user.getUpdatedDate());
		userDTO.setVerified(user.isVerified());
		return userDTO;
	}

}
