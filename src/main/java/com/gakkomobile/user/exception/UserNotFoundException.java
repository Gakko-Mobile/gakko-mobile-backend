package com.gakkomobile.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserNotFoundException extends RuntimeException {

    private String detail;
}
