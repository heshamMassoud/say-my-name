node {
  stage 'Checkout'
  checkout scm

  def mvnHome = tool 'M3'

  stage 'Build the JAR'
  sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean package"
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

  stage "Build docker image"
  def app = docker.build "heshamm/say-my-name:${env.BUILD_NUMBER}"

  stage "Publish docker images to docker registry"
  docker.withRegistry("https://registry.hub.docker.com", "docker-registry") {
      app.push()
      switch (env.BRANCH_NAME) {
        case "staging":
            app.push 'latest'
            break

        case "master":
            app.push 'production'
            break

        // Roll out a dev environment
        default:
            // Create namespace if it doesn't exist
            sh("kubectl get ns ${env.BRANCH_NAME} || kubectl create ns ${env.BRANCH_NAME}")
            // Don't use public load balancing for development branches
            sh("sed -i.bak 's#LoadBalancer#ClusterIP#' ./k8s/services/frontend.yaml")
            sh("sed -i.bak 's#gcr.io/cloud-solutions-images/gceme:1.0.0#${imageTag}#' ./k8s/dev/*.yaml")
            sh("kubectl --namespace=${env.BRANCH_NAME} apply -f k8s/services/")
            sh("kubectl --namespace=${env.BRANCH_NAME} apply -f k8s/dev/")
            echo 'To access your environment run `kubectl proxy`'
            echo "Then access your service via http://localhost:8001/api/v1/proxy/namespaces/${env.BRANCH_NAME}/services/${feSvcName}:80/"
      }
  }
}
