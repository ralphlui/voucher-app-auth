package voucher.management.app.auth.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.entity.User;
import voucher.management.app.auth.repository.UserRepository;
import voucher.management.app.auth.service.IUserService;
import voucher.management.app.auth.utility.DTOMapper;

@Service
public class UserService implements IUserService  {
	
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public Map<Long, List<UserDTO>> findByIsActiveTrue(Pageable pageable) {
		Map<Long, List<UserDTO>> result = new HashMap<>();
		List<UserDTO> userDTOList = new ArrayList<>();
		try {
			Page<User> userPages = userRepository.findByIsActiveTrue(pageable);
			long totalRecord = userPages.getTotalElements();
			if (totalRecord > 0) {
				for (User user : userPages.getContent()) {
					UserDTO userDTO = DTOMapper.toUserDTO(user);
					userDTOList.add(userDTO);
				}

			} else {
				logger.info("User not found...");
			}
			result.put(totalRecord, userDTOList);

		} catch (Exception ex) {
			logger.error("findByIsActiveTrue exception... {}", ex.toString());

		}
		return result;
	}

	@Override
	public UserDTO create(User user) {
		UserDTO userDTO = new UserDTO();
		try {
			String encodedPassword = passwordEncoder.encode(user.getPassword());
			user.setPassword(encodedPassword);
			String code = UUID.randomUUID().toString();
			user.setVerificationCode(code);
			user.setVerified(false);
			user.setActive(true);
			user.setCreatedDate(LocalDateTime.now());

			User createdUser = userRepository.save(user);

			if (createdUser != null) {
				//sendVerificationEmail(createdUser);
			}
			userDTO = DTOMapper.toUserDTO(user);

			return userDTO;

		} catch (Exception e) {
			logger.error("Error occurred while user creating, " + e.toString());
			e.printStackTrace();

		}

		return userDTO;
	}
	
	@Override
	public User findByEmail(String email) {

		return userRepository.findByEmail(email);
	}

}
