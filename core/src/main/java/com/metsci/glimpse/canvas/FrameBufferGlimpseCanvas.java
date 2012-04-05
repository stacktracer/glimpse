/*
 * Copyright (c) 2012, Metron, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Metron, Inc. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL METRON, INC. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.metsci.glimpse.canvas;

import static com.metsci.glimpse.gl.util.GLPBufferUtils.createPixelBuffer;

import java.awt.Dimension;
import java.util.List;

import javax.media.opengl.GLContext;

import com.metsci.glimpse.context.GlimpseBounds;
import com.metsci.glimpse.context.GlimpseContext;
import com.metsci.glimpse.context.GlimpseContextImpl;
import com.metsci.glimpse.context.GlimpseTarget;
import com.metsci.glimpse.context.GlimpseTargetStack;
import com.metsci.glimpse.gl.GLListenerInfo;
import com.metsci.glimpse.gl.GLRunnable;
import com.metsci.glimpse.gl.GLSimpleFrameBufferObject;
import com.metsci.glimpse.gl.GLSimpleListener;
import com.metsci.glimpse.gl.texture.DrawableTexture;
import com.metsci.glimpse.layout.GlimpseLayout;
import com.metsci.glimpse.support.settings.LookAndFeel;
import com.sun.opengl.util.texture.Texture;

/**
 * An offscreen GlimpseCanvas which renders its
 * {@link com.metsci.glimpse.painter.base.GlimpsePainter}s onto an
 * OpenGL texture which can then be captured and displayed elsewhere.
 *
 * @author ulman
 */
public class FrameBufferGlimpseCanvas implements GlimpseCanvas
{
    protected GLSimpleFrameBufferObject fbo;

    protected LayoutManager layoutManager;

    protected int width;
    protected int height;

    protected boolean isDisposed = false;

    public static interface GlimpseRunnable
    {
        public void run( GlimpseContext context );
    }

    public FrameBufferGlimpseCanvas( int width, int height, GLContext context )
    {
        GLContext newContext = createPixelBuffer( 1, 1, context ).getContext( );

        this.width = width;
        this.height = height;

        this.fbo = new GLSimpleFrameBufferObject( width, height, newContext );

        this.layoutManager = new LayoutManager( );

        this.fbo.addListener( new GLSimpleListener( )
        {
            @Override
            public void init( GLContext context )
            {
                // do nothing
            }

            @Override
            public void display( GLContext context )
            {
                for ( GlimpseLayout layout : layoutManager.getLayoutList( ) )
                {
                    layout.paintTo( getGlimpseContext( ) );
                }
            }

            @Override
            public void reshape( GLContext context, int x, int y, int width, int height )
            {
                resize0( );
            }

            @Override
            public void displayChanged( GLContext context, boolean modeChanged, boolean deviceChanged )
            {
                // do nothing
            }

            @Override
            public void dispose( GLContext context )
            {
                // do nothing
            }

            @Override
            public boolean isDisposed( )
            {
                // return dummy value
                return false;
            }

            @Override
            public GLListenerInfo getInfo( )
            {
                // return dummy value
                return null;
            }

        } );
    }

    protected void resize0( )
    {
        for ( GlimpseLayout layout : layoutManager.getLayoutList( ) )
        {
            layout.layoutTo( getGlimpseContext( ) );
        }
    }

    public void resize( int new_width, int new_height )
    {
        if ( this.width != new_width || this.height != new_height )
        {
            this.width = new_width;
            this.height = new_height;

            this.fbo.resize( this.width, this.height );
            this.resize0( );
        }
    }

    public GLSimpleFrameBufferObject getFrameBuffer( )
    {
        return this.fbo;
    }

    public Texture getOpenGLTexture( )
    {
        return fbo.getOpenGLTexture( );
    }

    public DrawableTexture getGlimpseTexture( )
    {
        return fbo.getGlimpseTexture( );
    }

    public Dimension getDimension( )
    {
        return fbo.getDimension( );
    }

    public void glSyncExec( final GlimpseRunnable runnable )
    {
        fbo.glSyncExec( new GLRunnable( )
        {
            @Override
            public Object run( GLContext context )
            {
                runnable.run( getGlimpseContext( ) );
                return null;
            }
        } );
    }

    @Override
    public GlimpseContext getGlimpseContext( )
    {
        return new GlimpseContextImpl( this );
    }

    @Override
    public GLContext getGLContext( )
    {
        return fbo.getGLContext( );
    }

    @Override
    public GlimpseBounds getTargetBounds( GlimpseTargetStack stack )
    {
        return new GlimpseBounds( getDimension( ) );
    }

    @Override
    public GlimpseBounds getTargetBounds( )
    {
        return getTargetBounds( null );
    }

    @Override
    public void addLayout( GlimpseLayout layout )
    {
        this.layoutManager.addLayout( layout );
    }

    @Override
    public void addLayout( GlimpseLayout layout, int zOrder )
    {
        this.layoutManager.addLayout( layout, zOrder );
    }

    @Override
    public void setZOrder( GlimpseLayout layout, int zOrder )
    {
        this.layoutManager.setZOrder( layout, zOrder );
    }

    @Override
    public void removeLayout( GlimpseLayout layout )
    {
        this.layoutManager.removeLayout( layout );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public List<GlimpseTarget> getTargetChildren( )
    {
        // layoutManager returns an unmodifiable list, thus this cast is typesafe
        // (there is no way for the recipient of the List<GlimpseTarget> view to
        // add GlimpseTargets which are not GlimpseLayouts to the list)
        return ( List ) this.layoutManager.getLayoutList( );
    }

    @Override
    public void setLookAndFeel( LookAndFeel laf )
    {
        for ( GlimpseLayout layout : layoutManager.getLayoutList( ) )
        {
            layout.setLookAndFeel( laf );
        }
    }
    
    @Override
    public void paint( )
    {
        fbo.draw( );
    }

    @Override
    public void dispose( )
    {
        GLContext context = getGLContext( );

        context.makeCurrent( );
        try
        {
            fbo.dispose( context );
        }
        finally
        {
            context.release( );
        }

        this.isDisposed = true;
    }

    @Override
    public boolean isDisposed( )
    {
        return this.isDisposed;
    }

    @Override
    public String toString( )
    {
        return FrameBufferGlimpseCanvas.class.getSimpleName( );
    }

    @Override
    public boolean isEventConsumer( )
    {
        return false;
    }

    @Override
    public void setEventConsumer( boolean consume )
    {
        // do nothing
    }

    @Override
    public boolean isEventGenerator( )
    {
        return false;
    }

    @Override
    public void setEventGenerator( boolean generate )
    {
        // do nothing
    }
}
