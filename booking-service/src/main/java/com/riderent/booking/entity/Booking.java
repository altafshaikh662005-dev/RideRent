package com.riderent.booking.entity;

import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name = "bookings") public class Booking { @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; @Column(nullable = false) private Long userId; @Column(nullable = false) private String vehicleName; @Column(nullable = false) private LocalDate bookingDate; @Column(nullable = false) private String status;
  public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getVehicleName(){return vehicleName;} public void setVehicleName(String v){vehicleName=v;} public LocalDate getBookingDate(){return bookingDate;} public void setBookingDate(LocalDate v){bookingDate=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
