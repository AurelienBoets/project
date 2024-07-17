package org.acme.service.grpc;

import java.util.ArrayList;
import java.util.List;
import org.acme.dto.order.CreateOrderDto;
import org.acme.dto.order.OrderDto;
import org.acme.dto.order.OrderItemDto;
import org.acme.dto.payment.CreatePaymentDto;
import org.acme.utils.VerifyLogin;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import order.AddOrder;
import order.Order;
import order.OrderGrpc;
import order.OrderId;
import order.OrderItem;
import order.Payment;
import order.UserId;

@Path("api/order")
public class OrderGrpcService {
    @GrpcClient
    OrderGrpc orderGrpc;

    @Inject
    VerifyLogin verifyLogin;

    @Inject
    @Channel("order-outgoing")
    Emitter<JsonObject> orderEmitter;

    @GET
    public Uni<Response> getAll(@Context HttpHeaders headers) {
        return verifyLogin.isLogin(headers).onItem().transformToUni(userId -> {
            if (userId == null) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return orderGrpc.getAllOrder(UserId.newBuilder().setId(userId).build()).onItem()
                    .transform(o -> {
                        List<OrderDto> orders = new ArrayList<>();
                        for (Order order : o.getOrdersList()) {
                            orders.add(orderGrpcToDto(order));
                        }
                        return Response.ok(orders).build();
                    });
        });
    }

    @GET
    @Path("/payment/{sessionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Fallback(fallbackMethod = "fallbackCreateOrder")
    @Timeout(value = 5000)
    public Uni<Response> createOrder(@Context HttpHeaders headers,
            @PathParam("sessionId") String sessionId) {
        return verifyLogin.isLogin(headers).onItem().transformToUni(userId -> {
            if (userId == null) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return orderGrpc
                    .createOrder(AddOrder.newBuilder().setStripeSession(sessionId).setIdUser(userId)
                            .build())
                    .onItem().transform(order -> Response.ok(orderGrpcToDto(order)).build());
        });
    }

    @POST
    @Path("/payment")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> createPayment(CreatePaymentDto paymentDto, @Context HttpHeaders headers) {
        return verifyLogin.isLogin(headers).onItem().transformToUni(userId -> {
            if (userId == null) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return orderGrpc
                    .createPayment(Payment.newBuilder().setTotalAmount(paymentDto.getTotalAmount())
                            .addAllOrderItems(orderItemDtoToGrpc(paymentDto.getItems()))
                            .setIdUser(userId).build())
                    .onItem().transform(url -> Response.ok(url.getUrl()).build());
        });
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") String id, @Context HttpHeaders headers) {
        return verifyLogin.isLogin(headers).onItem().transformToUni(userId -> {
            if (userId == null) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return orderGrpc.getOrder(OrderId.newBuilder().setId(id).build()).onItem()
                    .transform(order -> {
                        if (order.getIdUser() != userId) {
                            return Response.status(Response.Status.UNAUTHORIZED).build();
                        }
                        return Response.ok(orderGrpcToDto(order)).build();
                    });
        });
    }

    private OrderDto orderGrpcToDto(Order order) {
        List<OrderItemDto> items = new ArrayList<>();
        for (OrderItem item : order.getItemsList()) {
            items.add(new OrderItemDto(item.getProductId(), item.getProductName(),
                    item.getUnitPrice(), item.getPlatformId(), item.getPlatformName()));
        }
        return new OrderDto(order.getId(), order.getOrderDate(), order.getTotalAmount(),
                order.getIdUser(), items);
    }

    private List<OrderItem> orderItemDtoToGrpc(List<OrderItemDto> itemsDto) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemDto itemDto : itemsDto) {
            items.add(OrderItem.newBuilder().setProductId(itemDto.getProductId())
                    .setProductName(itemDto.getProductName()).setUnitPrice(itemDto.getUnitPrice())
                    .setPlatformId(itemDto.getPlatformId())
                    .setPlatformName(itemDto.getPlatformName()).build());
        }
        return items;
    }

    public Uni<Response> fallbackCreateOrder(@Context HttpHeaders headers,
            @PathParam("sessionId") String sessionId) {
        return verifyLogin.isLogin(headers).onItem().transformToUni(userId -> {
            if (userId == null) {
                return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            CreateOrderDto orderDto = new CreateOrderDto(userId, sessionId);
            System.out.println(orderDto);
            System.out.println(JsonObject.mapFrom(orderDto));
            orderEmitter.send(JsonObject.mapFrom(orderDto));
            return Uni.createFrom()
                    .item(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                            "Le service commande est actuellement indisponible. La commande sera créé dès que possible.")
                            .build());
        });
    }
}
