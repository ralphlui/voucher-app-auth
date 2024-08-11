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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import voucher.management.app.auth.dto.APIResponse;
import voucher.management.app.auth.dto.UserDTO;
import voucher.management.app.auth.service.impl.UserService;

import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api")
public class UserController {
	
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;

	@GetMapping(value = "/users", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllUser(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {
		logger.info("Call user getAll API with page={}, size={}", page, size);
		long totalRecord = 0;

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findByIsActiveTrue(pageable);

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

}
