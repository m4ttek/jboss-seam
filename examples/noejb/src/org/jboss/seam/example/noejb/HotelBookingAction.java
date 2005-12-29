//$Id$
package org.jboss.seam.example.noejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Outcome.REDISPLAY;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.hibernate.Session;
import org.hibernate.validator.Valid;
import org.jboss.logging.Logger;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Conversational;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.IfInvalid;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelectionIndex;
import org.jboss.seam.core.Conversation;

@Name("hotelBooking")
@Scope(CONVERSATION)
@Conversational(ifNotBegunOutcome="main")
@LoggedIn
public class HotelBookingAction implements Serializable
{
   private static final Logger log = Logger.getLogger(HotelBookingAction.class);
   
   @In(create=true)
   private Session bookingDatabase;
   
   private String searchString;
   
   @DataModel
   private List<Hotel> hotels;
   @DataModelSelectionIndex
   int hotelIndex = 0;
   
   @Out(required=false)
   private Hotel hotel;
   
   @In(required=false) 
   @Out(required=false)
   @Valid
   private Booking booking;
   
   @In
   private User user;
      
   @In
   private transient FacesContext facesContext;
   
   @In(required=false)
   private transient BookingListAction bookingList;
   
   @In(create=true)
   private transient Conversation conversation;
   
   public String getSearchString()
   {
      return searchString;
   }

   public void setSearchString(String searchString)
   {
      this.searchString = searchString;
   }

   @Begin(join=true)
   public String find()
   {
      hotel = null;
      String searchPattern = searchString==null ? "%" : '%' + searchString.toLowerCase().replace('*', '%') + '%';
      hotels = bookingDatabase.createQuery("from Hotel where lower(name) like :search or lower(city) like :search or lower(zip) like :search or lower(address) like :search")
            .setParameter("search", searchPattern)
            .setMaxResults(50)
            .list();
      
      log.info(hotels.size() + " hotels found");
      
      return conversation.switchableOutcome("main", "Search hotels: " + searchString);
   }
   
   public String selectHotel()
   {
      if ( hotels==null ) return "main";
      setHotel();
      return conversation.switchableOutcome("selected");
   }

   public String nextHotel()
   {
      if ( hotelIndex<hotels.size()-1 )
      {
         ++hotelIndex;
         setHotel();
      }
      return null;
   }

   public String lastHotel()
   {
      if (hotelIndex>0)
      {
         --hotelIndex;
         setHotel();
      }
       return null;
   }

   private void setHotel()
   {
      log.info( "hotel selected: " + hotelIndex + "=>" + hotel );
      hotel = hotels.get(hotelIndex);
      conversation.setDescription( "View hotel: " + hotel.getName() );
   }
   
   public String bookHotel()
   {
      if (hotel==null) return "main";
      
      booking = new Booking(hotel, user);
      Calendar calendar = Calendar.getInstance();
      booking.setCheckinDate( calendar.getTime() );
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      booking.setCheckoutDate( calendar.getTime() );
      
      return conversation.switchableOutcome( "book", "Book hotel: " + hotel.getName() );
   }
   
   @IfInvalid(outcome=REDISPLAY)
   public String setBookingDetails()
   {
      if (booking==null || hotel==null) return "main";
      
      if ( !booking.getCheckinDate().before( booking.getCheckoutDate() ) )
      {
         log.info("invalid booking dates");
         FacesMessage facesMessage = new FacesMessage("Check out date must be later than check in date");
         facesContext.addMessage(null, facesMessage);
         return null;
      }
      else
      {
         log.info( "valid booking: " + booking.getDescription() );
         return conversation.switchableOutcome( "confirm", "Confirm booking: " + booking.getDescription() );
      }
   }
      
   @End
   public String confirm()
   {
      if (booking==null || hotel==null) return "main";
      
      bookingDatabase.persist(booking);
      if (bookingList!=null) bookingList.refresh();
      log.info("booking confirmed");
      return "confirmed";
   }
   
   @End
   public String clear()
   {
      hotels = null;
      hotel = null;
      booking = null;
      return "main";
   }

}
