package com.riderent.booking.dto;

import java.time.LocalDate;

 import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public final class BookingDtos { private BookingDtos(){} public record BookingRequest(@NotNull Long userId,@NotBlank String vehicleName,@NotNull LocalDate bookingDate,@NotBlank String status){} public record BookingResponse(Long id,Long userId,String vehicleName,LocalDate bookingDate,String status){} }
