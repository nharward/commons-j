// Copyright 2011 Nathaniel Harward
//
// This file is part of commons-j.
//
// commons-j is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// commons-j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with commons-j. If not, see <http://www.gnu.org/licenses/>.
package us.harward.commons.net.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * Class that only allows objects that pass the supplied filter to move downstream.
 */
public final class DownstreamMessageFilteringHandler extends SimpleChannelDownstreamHandler {

    private final Predicate<Object> filter;

    public DownstreamMessageFilteringHandler(final Predicate<Object> filter) {
        Preconditions.checkNotNull(filter);
        this.filter = filter;
    }

    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        if (filter.apply(e.getMessage()))
            super.writeRequested(ctx, e);
    }

}
