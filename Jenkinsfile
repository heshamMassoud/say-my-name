node {
  stage 'Checkout'
  checkout scm

  def mvnHome = tool 'M3'

  stage 'Build the JAR'
  sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean package"
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

  stage "Build docker image"
  def pom = readMavenPom file: 'pom.xml'
  def appVersion = pom.version
  def imageTag = "heshamm/say-my-name:${appVersion}"
  def dockerImage = docker.build imageTag

  stage "Publish docker images to docker registry"
  docker.withRegistry("https://registry.hub.docker.com", "docker-registry") {
      dockerImage.push()
      switch (env.BRANCH_NAME) {
        case "staging":
            dockerImage.push 'staging'
            stage "Deploying images to Kubernetes cluster"
            // Create namespace if it doesn't exist
            sh("kubectl get ns staging || kubectl create ns staging")
            sh("sed -i.bak 's#heshamm/say-my-name:latest#${imageTag}#' ./k8s/deployments/staging/*.yaml")
            sh("sed -i.bak 's#heshamm/say-my-name:latest#${imageTag}#' ./k8s/services/staging/*.yaml")
            sh("kubectl --namespace=staging apply -f k8s/services/staging")
            sh("kubectl --namespace=staging apply -f k8s/deployments/staging")
            def serviceName = "say-my-name-frontend-staging"
            sh("echo http://`kubectl --namespace=staging get service/${serviceName} --output=json | jq -r '.status.loadBalancer.ingress[0].ip'` > ${serviceName}")
            break
        case "master":
            dockerImage.push 'production'
            stage "Deploying images to Kubernetes cluster"
            // Create namespace if it doesn't exist
            sh("kubectl get ns production || kubectl create ns production")
            sh("sed -i.bak 's#heshamm/say-my-name:latest#${imageTag}#' ./k8s/deployments/production/*.yaml")
            sh("sed -i.bak 's#heshamm/say-my-name:latest#${imageTag}#' ./k8s/services/production/*.yaml")
            sh("kubectl --namespace=production apply -f k8s/services/production")
            sh("kubectl --namespace=production apply -f k8s/deployments/production")
            def serviceName = "say-my-name-frontend-production"
            sh("echo http://`kubectl --namespace=production get service/${serviceName} --output=json | jq -r '.status.loadBalancer.ingress[0].ip'` > ${serviceName}")
            break
      }
  }
}
