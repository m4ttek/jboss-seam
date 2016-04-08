package org.jboss.seam.ui.facelet;

import static org.jboss.seam.ScopeType.APPLICATION;

import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.apache.myfaces.view.facelets.compiler.SAXCompiler;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;

import javax.faces.context.FacesContext;
import java.lang.reflect.Method;

//import com.sun.faces.application.ApplicationAssociate;

@Name("org.jboss.seam.ui.faces.facelet.faceletCompiler")
@Scope(APPLICATION)
@BypassInterceptors
@AutoCreate
@Install(value = true, precedence = Install.BUILT_IN, classDependencies="javax.faces.view.facelets.Facelet")
//@Install(value = true, precedence = Install.BUILT_IN, classDependencies="javax.faces.view.facelets.Facelet")
public class FaceletCompiler
{
   private static final LogProvider log = Logging.getLogProvider(FaceletCompiler.class);
   private org.apache.myfaces.view.facelets.compiler.Compiler compiler;
   
   @Create
   public void create()
   {



//	   ApplicationAssociate applicationAssociate = ApplicationAssociate.getCurrentInstance();
//	   if (applicationAssociate != null)
//	   {
//		   compiler = applicationAssociate.getCompiler();
//	   }
//	   else
//	   {
//		   // TODO: this requires to initialize custom tag library
//		   compiler = new SAXCompiler();
//	   }

      try {
         FacesContext facesContext = FacesContext.getCurrentInstance();
         FaceletViewDeclarationLanguage vdl = new FaceletViewDeclarationLanguage(facesContext);
         Method createCompiler = createCompiler = FaceletViewDeclarationLanguage.class.getDeclaredMethod("createCompiler",
                 FacesContext.class);

         createCompiler.setAccessible(true);

         compiler = (org.apache.myfaces.view.facelets.compiler.Compiler) createCompiler
                 .invoke(vdl, facesContext);
      } catch (Exception ex) {
         compiler = new SAXCompiler();
         log.error("Error creating FaceletCompiler. returning SAXCompiler instead", ex);
      }


   }
     
   
   @Unwrap
   public org.apache.myfaces.view.facelets.compiler.Compiler unwrap()
   {
      return compiler;
   }
   
   public static org.apache.myfaces.view.facelets.compiler.Compiler instance()
   {
      if ( !Contexts.isApplicationContextActive() )
      {
         throw new IllegalStateException("No active application scope");
      }
      return (org.apache.myfaces.view.facelets.compiler.Compiler) Component.getInstance(FaceletCompiler.class, ScopeType.APPLICATION);
   }
   
}

