package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
@RegisterForReflection
public class LoginResponseDto {
    private Long id;
}
