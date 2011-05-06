/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.chain;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.context.notification.MessageProcessorNotification;

import java.util.Arrays;
import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{

    protected DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {   
        super(null, processors);
    }

    protected DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, Arrays.asList(processors));
    }

    protected DefaultMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    protected DefaultMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    public static MessageProcessorChain from(MessageProcessor messageProcessor)
    {
        return new DefaultMessageProcessorChain(messageProcessor);
    }

    public static MessageProcessorChain from(MessageProcessor... messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    public static MessageProcessorChain from(List<MessageProcessor> messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }
    
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        for (MessageProcessor processor : processors)
        {
            fireNotification(event.getFlowConstruct(), event, processor, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);

            resultEvent = processor.process(currentEvent);

            fireNotification(event.getFlowConstruct(), resultEvent, processor,
                MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);

            if (resultEvent != null)
            {
                currentEvent = resultEvent;
            }
            else
            {
                // MessageProcessor returned null
                // We need to determine what we should use as input for next processor
                if (flowConstruct instanceof SimpleFlowConstruct)
                {
                    // In a flow when a MessageProcessor returns null the next processor acts as an implicit 
                    // branch receiving a copy of the message used for previous MessageProcessor
                    currentEvent = OptimizedRequestContext.criticalSetEvent(currentEvent);
                }
                else
                {
                    // But in a service we don't do any implicit branching.
                    return null;
                }
            }
        }
        return currentEvent;
    }

}
