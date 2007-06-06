package org.jboss.seam.core;

import java.io.Serializable;
import java.util.Date;

/**
 * A "schedule" for a timed event executed by
 * a timer service which supports delayed
 * timed events. It is the base class for the more
 * useful TimerSchedule and CronSchedule classes.
 * 
 * @author Michael Yuan
 *
 */
public class Schedule implements Serializable
{
   private Long duration;
   private Date expiration;
   
   Long getDuration()
   {
      return duration;
   }
   
   Date getExpiration()
   {
      return expiration;
   }
   
   /**
    * @param duration the delay before the event occurs
    * @param expiration the datetime at which the event occurs
    */
   Schedule(Long duration, Date expiration)
   {
      this.duration = duration;
      this.expiration = expiration;
   }

   /**
    * @param duration the delay before the event occurs
    */
   public Schedule(Long duration)
   {
      this.duration = duration;
   }

   /**
    * @param expiration the datetime at which the event occurs
    */
   public Schedule(Date expiration)
   {
      this.expiration = expiration;
   }

   public Schedule () { }

}
