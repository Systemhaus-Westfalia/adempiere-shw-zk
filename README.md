# SHW ZK Customizations
Éste proyecto se encarga de construir y publicar un ZK personalizado basado en parches generados sobre el ADempiere Base y sobre el Swing si lo deseas, depende directamente de [zk-ui](https://github.com/adempiere/zk-ui) y de [shw-customizations](https://github.com/Systemhaus-Westfalia/shw-customizations).


## ¿Cómo usarlo?
El resultado de éste proyecto es una imagen de ZK + parches internos de SHW, por lo tanto es importante saber cómo se generan y donde se publican los parches generados.

### ¿Dónde se generan los paquetes y parches?
Los paquetes ya con patches y librerías de SHW los puedes ver [aquí](https://github.com/orgs/Systemhaus-Westfalia/packages?repo_name=shw-customizations)

Para hacer uso de las librerías personalizadas hay que hacer un cambio en el repositorio y apuntar al proyecto donde se encuentra la [librería](https://maven.pkg.github.com/Systemhaus-Westfalia/shw-customizations), esto se puede hacer como se muestra a continuación:

```Gradle
maven {
    	url = "https://maven.pkg.github.com/Systemhaus-Westfalia/shw-customizations"
        credentials {
        	username = System.getenv("GITHUB_DEPLOY_USER") ?: System.properties['deploy.user']
            password = System.getenv("GITHUB_DEPLOY_TOKEN") ?: System.properties['deploy.token'] 
		}
	}
```

Las librerías completas son estas: `org.shw.shw-customizations.shw_libs`


Como se ve en el proyecto sólo se llama a una sola librería porque ella tiene todos los paquetes base de adempiere mas los parches:

```Gradle
implementation 'org.shw:shw-customizations.shw_libs:1.0.1'
```

Las imágenes del ZK con patches se publican ya automáticamente [aquí](https://hub.docker.com/r/marcalwestf/shw-zk-customizations/tags)

Si se desea cambiar la versión base de zk, que es la correspondiente al proyecto [zk-ui](https://github.com/adempiere/zk-ui), simplemente hay que cambiarla en ésta variable `adempiereZKVersion`

## Consideraciones importantes
Dependiendo del nivel del parche hay cosas que considerar:

- Si el parche es sobre ZK, simplemente basta con agregarlo en el directorio `zkwebui/WEB-INF/src` y dentro de él debe estar la estructura de directorios como se encuentran los paquetes. Ejemplo: Para un parche en la clase `org.adempiere.webui.panel.AbstractADWindowPanel.java` debería estar ubicado en `zkwebui/WEB-INF/src/org/adempiere/webui/panel/AbstractADWindowPanel.java`
  - Después de agregarlo simplemente se hace commit, se verifica que construya y se genera release
- Si el parche es sobre una librería base ejemplo `org.compiere.model.CalloutPayment.java`:
  - Se debe agregar ese parche en el proyecto [shw-customizations](https://github.com/Systemhaus-Westfalia/shw-customizations)
  - Se debe generar release en el proyecto [shw-customizations](https://github.com/Systemhaus-Westfalia/shw-customizations) y esperar a que construya y publique el release
  - Luego se debe modificar el archivo `build.gradle` de éste proyecto con la nueva versión que se generó del proyecto [shw-customizations](https://github.com/Systemhaus-Westfalia/shw-customizations)
  - Se hace commit sobre éste proyecto
  - Se genera release con la nueva imagen

## A tomar en cuenta
Éste es un proyecto cascarón que sólo tiene como fuente los parches que se agregan a ZK estríctamente y el resto viene del proyecto originalmente creado de [zk-ui](https://github.com/adempiere/zk-ui) + las librerías y parches generados en el proyecto [shw-customizations](https://github.com/Systemhaus-Westfalia/shw-customizations)