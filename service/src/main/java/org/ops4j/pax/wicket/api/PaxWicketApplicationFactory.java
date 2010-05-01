/*
 * Copyright 2006 Niclas Hedhman.
 * Copyright 2006 Edward F. Yakop
 * Copyright 2008 David Leangen
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.api;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import org.apache.wicket.Page;
import org.apache.wicket.application.IClassResolver;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.IWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import static org.ops4j.lang.NullArgumentException.validateNotEmpty;
import static org.ops4j.lang.NullArgumentException.validateNotNull;
import static org.ops4j.pax.wicket.api.ContentSource.APPLICATION_NAME;
import static org.ops4j.pax.wicket.api.ContentSource.HOMEPAGE_CLASSNAME;
import static org.ops4j.pax.wicket.api.ContentSource.MOUNTPOINT;
import org.ops4j.pax.wicket.internal.BundleDelegatingClassResolver;
import org.ops4j.pax.wicket.internal.DelegatingClassResolver;
import org.ops4j.pax.wicket.internal.PaxAuthenticatedWicketApplication;
import org.ops4j.pax.wicket.internal.PaxWicketApplication;
import org.ops4j.pax.wicket.internal.PaxWicketPageFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public final class PaxWicketApplicationFactory
    implements IWebApplicationFactory, ManagedService
{

    private static final String[] APPLICATION_FACTORY_SERVICE_NAMES = {
        PaxWicketApplicationFactory.class.getName(), ManagedService.class.getName()
    };

    private final BundleContext m_bundleContext;
    private Class<? extends Page> m_homepageClass;
    private final Properties m_properties;

    private PaxWicketPageFactory m_pageFactory;
    private DelegatingClassResolver m_delegatingClassResolver;

    private ServiceRegistration m_registration;

    private PaxWicketAuthenticator m_authenticator;
    private Class<? extends WebPage> m_signinPage;

    private PageMounter m_pageMounter;
    private RequestCycleProcessorFactory m_requestCycleProcessorFactory;
    private SessionStoreFactory m_sessionStoreFactory;

    private List<IComponentInstantiationListener> m_componentInstantiationListeners;

    private ServiceRegistration m_bdcrRegistration;
    private BundleDelegatingClassResolver m_bdcr;

    /**
     * Construct an instance of {@code PaxWicketApplicationFactory} with the specified arguments.
     *
     * @param context         The bundle context. This argument must not be {@code null}.
     * @param homepageClass   The homepage class. This argument must not be {@code null}.
     * @param mountPoint      The mount point. This argument must not be be {@code null}.
     * @param applicationName The application name. This argument must not be {@code null}.
     *
     * @throws IllegalArgumentException Thrown if one or some or all arguments are {@code null}.
     * @since 1.0.0
     */
    public PaxWicketApplicationFactory(
        BundleContext context,
        Class<? extends Page> homepageClass,
        String mountPoint,
        String applicationName )
        throws IllegalArgumentException
    {
        validateNotNull( context, "context" );
        validateNotNull( homepageClass, "homepageClass" );
        validateNotNull( mountPoint, "mountPoint" );
        validateNotEmpty( applicationName, "applicationName" );

        m_properties = new Properties();

        m_homepageClass = homepageClass;
        m_bundleContext = context;

        setMountPoint( mountPoint );
        setApplicationName( applicationName );

        String homepageClassName = homepageClass.getName();
        m_properties.setProperty( HOMEPAGE_CLASSNAME, homepageClassName );

        m_componentInstantiationListeners = new ArrayList<IComponentInstantiationListener>();
    }

    /**
     * Sets the authenticator of this pax application factory.
     * <p>
     * Note: Value changed will only affect wicket application created after this method invocation.
     * </p>
     *
     * @param authenticator The authenticator.
     * @param signInPage    The sign in page.
     *
     * @throws IllegalArgumentException Thrown if one of the arguments are {@code null}.
     * @see #register()
     * @since 1.0.0
     */
    public final void setAuthenticator( PaxWicketAuthenticator authenticator, Class<? extends WebPage> signInPage )
        throws IllegalArgumentException
    {
        if( ( authenticator != null && signInPage == null ) || ( authenticator == null && signInPage != null ) )
        {
            String message = "Both [authenticator] and [signInPage] argument must not be [null].";
            throw new IllegalArgumentException( message );
        }

        m_authenticator = authenticator;
        m_signinPage = signInPage;
    }

    /**
     * Clear resources used by this {@code PaxWicketApplicationFactory} instance.
     * <p>
     * Note: dispose does not unregister this {@code PaxWicketApplicationFactory} instance from the OSGi container.
     * </p>
     *
     * @since 1.0.0
     */
    public final void dispose()
    {
        synchronized( this )
        {
            m_pageFactory.dispose();
            m_delegatingClassResolver.dispose();

            if( m_bdcrRegistration != null )
            {
                m_bdcrRegistration.unregister();
            }
        }
    }

    /**
     * Returns the mount point that the wicket application will be accessible. This method must not return {@code null}
     * string.
     *
     * @return The mount point that the wicket application will be accessible.
     *
     * @since 1.0.0
     */
    public final String getMountPoint()
    {
        synchronized( this )
        {
            return m_properties.getProperty( MOUNTPOINT );
        }
    }

    @SuppressWarnings( "unchecked" )
    public final void updated( Dictionary config )
        throws ConfigurationException
    {
        if( config == null )
        {
            synchronized( this )
            {
                m_registration.setProperties( m_properties );
            }

            return;
        }

        String classname = (String) config.get( HOMEPAGE_CLASSNAME );
        if( classname == null )
        {
            synchronized( this )
            {
                String homepageClassName = m_homepageClass.getName();
                config.put( HOMEPAGE_CLASSNAME, homepageClassName );
            }
        }
        else
        {
            synchronized( this )
            {
                try
                {
                    Bundle bundle = m_bundleContext.getBundle();
                    m_homepageClass = bundle.loadClass( classname );
                }
                catch( ClassNotFoundException e )
                {
                    throw new ConfigurationException( HOMEPAGE_CLASSNAME, "Class not found in application bundle.", e );
                }
            }
        }

        String applicationName = (String) config.get( APPLICATION_NAME );
        if( applicationName == null )
        {
            String currentApplicationName;
            synchronized( this )
            {
                currentApplicationName = (String) m_properties.get( APPLICATION_NAME );
            }

            config.put( APPLICATION_NAME, currentApplicationName );
        }
        else
        {
            setApplicationName( applicationName );
        }

        String mountPoint = (String) config.get( MOUNTPOINT );
        if( mountPoint == null )
        {
            String currentMountPoint;
            synchronized( this )
            {
                currentMountPoint = m_properties.getProperty( MOUNTPOINT );
            }
            config.put( MOUNTPOINT, currentMountPoint );
        }
        else
        {
            setMountPoint( mountPoint );
        }

        m_registration.setProperties( config );
    }

    /**
     * Register this {@code PaxWicketApplicationFactory} instance to OSGi container.
     *
     * @return The service registration.
     *
     * @since 1.0.0
     */
    public final ServiceRegistration register()
    {
        Properties serviceProperties;
        synchronized( this )
        {
            serviceProperties = new Properties( m_properties );
        }
        m_registration = m_bundleContext.registerService( APPLICATION_FACTORY_SERVICE_NAMES, this, serviceProperties );

        return m_registration;
    }

    private void setApplicationName( String applicationName )
    {
        synchronized( this )
        {
            if( m_pageFactory != null )
            {
                m_pageFactory.dispose();
            }
            if( m_delegatingClassResolver != null )
            {
                m_delegatingClassResolver.dispose();
            }

            m_delegatingClassResolver = new DelegatingClassResolver( m_bundleContext, applicationName );
            m_delegatingClassResolver.intialize();

            m_pageFactory = new PaxWicketPageFactory( m_bundleContext, applicationName );
            m_pageFactory.initialize();

            m_properties.setProperty( APPLICATION_NAME, applicationName );
        }
    }

    private void setMountPoint( String mountPoint )
    {
        synchronized( this )
        {
            m_properties.put( MOUNTPOINT, mountPoint );
        }
    }

    public final void setPageMounter( PageMounter pageMounter )
    {
        validateNotNull( pageMounter, "pageMounter" );

        m_pageMounter = pageMounter;
    }

    public void setRequestCycleProcessorFactory( RequestCycleProcessorFactory factory )
    {
        m_requestCycleProcessorFactory = factory;
    }

    public void setSessionStoreFactory( SessionStoreFactory sessionStoreFactory )
    {
        m_sessionStoreFactory = sessionStoreFactory;
    }

    public final void setPaxWicketBundle( Bundle bundle )
    {
        if( m_bdcrRegistration != null )
        {
            m_bdcrRegistration.unregister();
            m_bdcrRegistration = null;
        }

        if( m_bdcr != null )
        {
            m_bdcr.close();
        }

        if( bundle != null )
        {
            Properties config = new Properties();
            String applicationName = getApplicationName();
            config.setProperty( APPLICATION_NAME, applicationName );

            m_bdcr = new BundleDelegatingClassResolver( m_bundleContext, applicationName, bundle );
            m_bdcrRegistration = m_bundleContext.registerService( IClassResolver.class.getName(), m_bdcr, config );
        }
    }

    /**
     * Returns the application name.
     *
     * @return The application name.
     *
     * @since 0.5.4
     */
    private String getApplicationName()
    {
        return m_properties.getProperty( APPLICATION_NAME );
    }

    public void addComponentInstantiationListener( IComponentInstantiationListener listener )
    {
        m_componentInstantiationListeners.add( listener );
    }

    /**
     * Creates a web application.
     *
     * @param filter The wicket filter.
     *
     * @return The new web application.
     */
    public final WebApplication createApplication( WicketFilter filter )
    {
        WebApplication paxWicketApplication;

        synchronized( this )
        {
            String applicationName = getApplicationName();

            if( m_authenticator != null && m_signinPage != null )
            {
                paxWicketApplication = new PaxAuthenticatedWicketApplication(
                    m_bundleContext, applicationName, m_pageMounter, m_homepageClass, m_pageFactory,
                    m_requestCycleProcessorFactory, m_sessionStoreFactory, m_delegatingClassResolver, 
                    m_authenticator, m_signinPage
                );
            }
            else
            {
                paxWicketApplication = new PaxWicketApplication(
                    m_bundleContext, applicationName, m_pageMounter, m_homepageClass, m_pageFactory,
                    m_delegatingClassResolver
                );
            }
        }

        for( IComponentInstantiationListener listener : m_componentInstantiationListeners )
        {
            paxWicketApplication.addComponentInstantiationListener( listener );
        }

        return paxWicketApplication;
    }
}