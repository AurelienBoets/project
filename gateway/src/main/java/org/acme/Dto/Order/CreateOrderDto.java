package org.acme.dto.order;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateOrderDto {
    private Long idUser;
    private String sessionId;
}
