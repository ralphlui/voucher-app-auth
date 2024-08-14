package voucher.management.app.auth.dto;

import voucher.management.app.auth.enums.RoleType;

public class UserResponse {


		private String email;
		private String username;
		private RoleType role;

		public UserResponse() {
			super();
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public RoleType getRole() {
			return role;
		}

		public void setRole(RoleType role) {
			this.role = role;
		}

	}


