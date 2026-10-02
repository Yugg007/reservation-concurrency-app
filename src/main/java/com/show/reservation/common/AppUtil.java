package com.show.reservation.common;

import java.util.UUID;

public class AppUtil {
	public static UUID parse(String s) {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			throw new ApiException(404, "not_found", "invalid id");
		}
	}

}
