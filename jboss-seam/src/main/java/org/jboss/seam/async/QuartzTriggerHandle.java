package org.jboss.seam.async;

import java.io.Serializable;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Provides control over the Quartz Job.
 * 
 * @author Michael Yuan
 *
 */
public class QuartzTriggerHandle implements Serializable
{
   private final TriggerKey triggerKey;
   
   // Hold a transient reference to the scheduler to allow control of the
   // scheduler outside of Seam contexts (useful in a testing context)
   private transient Scheduler scheduler;
     
   public QuartzTriggerHandle(TriggerKey triggerKey)
   {
      this.triggerKey = triggerKey;
   }

   public void cancel() throws SchedulerException
   {
      getScheduler().unscheduleJob(triggerKey);
   }
   
   public void pause() throws SchedulerException
   {
      getScheduler().pauseTrigger(triggerKey);
   }
   
   public Trigger getTrigger() throws SchedulerException
   {
      return getScheduler().getTrigger(triggerKey);
   }
   
   public void resume() throws SchedulerException
   {
      getScheduler().resumeTrigger(triggerKey);
   }
   
   private Scheduler getScheduler()
   {
       if (scheduler == null)
       {
           scheduler = QuartzDispatcher.instance().getScheduler();
       }
       return scheduler;
   }
   
}
  