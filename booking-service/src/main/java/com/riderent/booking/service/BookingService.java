package com.riderent.booking.service;

import java.util.List;

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
 import org.springframework.stereotype.Service;
 import org.springframework.web.client.RestClient;
 import org.springframework.web.server.ResponseStatusException;

 import com.riderent.booking.dto.BookingDtos.*;
 import com.riderent.booking.dto.BookingDtos.BookingRequest;
import com.riderent.booking.dto.BookingDtos.BookingResponse;
import com.riderent.booking.entity.Booking;
import com.riderent.booking.repository.BookingRepository;
@Service public class BookingService { private final BookingRepository repo; private final RestClient client; private final String userUrl; public BookingService(BookingRepository repo,RestClient client,@Value("${user-service.url}")String userUrl){this.repo=repo;this.client=client;this.userUrl=userUrl;}
  public BookingResponse create(BookingRequest r,String auth){try{client.get().uri(userUrl+"/api/users/{id}",r.userId()).header(HttpHeaders.AUTHORIZATION,auth).retrieve().toBodilessEntity();}catch(org.springframework.web.client.HttpClientErrorException.NotFound e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found");}catch(org.springframework.web.client.RestClientException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"User service unavailable");} Booking b=new Booking(); b.setUserId(r.userId()); b.setVehicleName(r.vehicleName()); b.setBookingDate(r.bookingDate()); b.setStatus(r.status()); return response(repo.save(b)); }
  public BookingResponse get(Long id){return response(repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found")));} public List<BookingResponse> byUser(Long id){return repo.findByUserId(id).stream().map(this::response).toList();} public BookingResponse cancel(Long id){Booking b=repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found"));b.setStatus("CANCELLED");return response(repo.save(b));} private BookingResponse response(Booking b){return new BookingResponse(b.getId(),b.getUserId(),b.getVehicleName(),b.getBookingDate(),b.getStatus());}
}
