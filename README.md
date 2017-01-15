# say-my-name
A spring-boot microservice to demonstrate docker container orchestration using a hosted kubernetes cluster on Google Cloud's Container Engine.

###About the sample microservice
Production: http://104.155.85.38/say/there

Staging: http://130.211.93.195/say/there


###What the Prototype Covers
1. A deployed instance of Jenkins server on a GKE (Google Container Engine) Kubernetes cluster.
2. Setup of a multi-branched pipeline on Jenkins.
3. Setup of a production and staging environment for the microservice on the kubernetes cluster.
4. Development workflow and release to production (and how to switch it to canary-releasing).
5. Scaling (Manual horizontal scaling, vertical and auto-scaling)
6. Monitoring and logging using Stackdriver.
7. Dynamic Configuration.
8. Advantages of using Kubernetes and/or Kubernetes on GKE.

###Bird's Eye View
![say-my-name-birds-eye](https://cloud.githubusercontent.com/assets/9512131/21627687/cbf7da7e-d219-11e6-933e-34e9da3c4abd.png)

###Environment Isolation
Currently the staging environment is totally isolated from the production environment using kubernetes namespaces to
have pod-level isolation
```bash
kubectl --namespace=staging apply -f k8s/services/staging
kubectl --namespace=staging apply -f k8s/deployments/staging
```
The services know which pods to connect to using labels and selectors as follows:
######The Pod
```yaml
labels:
    app: say-my-name
    role: frontend
    env: staging
```
######The Service
```yaml
selector:
    app: say-my-name
    role: frontend
    env: staging
```
###Canary-Releases
It is also possible to have the staging environment set as a canary release environment, in which case 
both environments would have the same namespace and only updating the staging container images. This would lead, for example,
 to having a new update seen by 20% of the users and if it's okay, it can be rolled out to the production pods.
###Jenkins Multi-branched Pipeline
When code is pushed on github to the staging branch a build is triggered on Jenkins with the following stages, based
 on the [Jenkinsfile](https://github.com/heshamMassoud/say-my-name/blob/master/Jenkinsfile), as seen in the screenshot below:
 
 1. Checkout the code from the github repo.
 2. Maven compiles, tests and packages the microservice into a JAR file.
 3. Based on the [Dockerfile](https://github.com/heshamMassoud/say-my-name/blob/master/Dockerfile) an image is built.
 4. The docker image build is pushed (with several tags) to the specified container registry.
 5. Based on the [Kubernetes resources' yaml files](https://github.com/heshamMassoud/say-my-name/tree/master/k8s), the images are deployed on the `staging` namespaced cluster.
 
When the code is pushed to the master branch the steps are executed, except the docker images are deployed on the 
`production` namespace.


<img width="880" alt="screen shot 2017-01-03 at 19 12 56" src="https://cloud.githubusercontent.com/assets/9512131/21618084/80643c54-d1e9-11e6-9c28-265f714081bc.png">


###Scaling
#####Manual horizontal scaling of production environment pods
```bash
kubectl --namespace=production scale deployment say-my-name-frontend-production --replicas=4
```
#####Horizonal Pod Autoscaling in Kubernetes
With Horizontal Pod Autoscaling, Kubernetes automatically scales the number of pods in a replication controller, deployment or replica set based on observed CPU utilization (or, with alpha support, on some other, application-provided metrics).
The autoscalar periodically queries CPU utilization for the pods it targets. (The period of the autoscaler is controlled by `--horizontal-pod-autoscaler-sync-period` flag of controller manager. The default value is 30 seconds). Then, it compares the arithmetic mean of the pods’ CPU utilization with the target and adjust the number of replicas if needed.

The following command will create a Horizontal Pod Autoscaler that maintains between 2 and 10 replicas of the Pods controlled by the
`say-my-name-frontend-production` deployment.
```bash
kubectl --namespace=production autoscale deployment say-my-name-frontend-production --cpu-percent=50 --min=2 --max=10
```
To check the status of the autoscaler run:
```bash
kubectl get hpa
```
HPA scales up or down the number of replicas according to the specified percentage compared to the actual load.

#####Cluster Autoscaling in GKE

Cluster Autoscaler enables users to automatically resize clusters so that all scheduled pods have a place to run.
 If there are no resources in the cluster to schedule a recently created pod, a new node is added. On the other hand, 
 if some node is underutilized and all pods running on it can be easily moved elsewhere then the node is deleted.
 Feature is, however, still in [beta](https://cloud.google.com/container-engine/docs/cluster-autoscaler).

###Useful commands
####Manually setup a kubernetes .yaml deployment
```bash
kubectl --namespace=staging apply -f k8s/deployments/staging/
```

####Status of created services and pods
```bash
kubectl --namespace="staging" get services
kubectl --namespace="staging" get pods
```
####Details of created services and pods
```bash
kubectl --namespace="staging" describe services
kubectl --namespace="staging" describe pods
```

##Monitoring, logging and Error Reporting
Using the kubernetes resource labels e.g. 
````yaml
    app: say-my-name
    role: frontend
    env: staging
````
Through the Google console [Stackdriver](https://app.google.stackdriver.com/monitoring/1039756/say-my-name-cpu-and-disk-usage?project=say-my-name-154414)
you can choose a label or a combination of labels to monitor the nodes your pods are running on, as seen in the screenshot below. You can monitor the CPU usage of all nodes running production pods by simply filtering by the label `env: production`
or aggregate the average CPU usage of nodes exposing the production frontend pods for example.
![screen shot 2017-01-03 at 21 06 48](https://cloud.githubusercontent.com/assets/9512131/21621291/a18d7cba-d1f8-11e6-9a02-8849eaad9160.png)

Using resource labels, you can filter the logs only coming from the components, apps, pods, nodes or clusters you wish 
as seen in the screenshot below.

![screen shot 2017-01-03 at 21 26 01](https://cloud.githubusercontent.com/assets/9512131/21623320/17abce16-d202-11e6-93c7-8708a6fe1e2a.png)
![screen shot 2017-01-03 at 21 25 30 1](https://cloud.githubusercontent.com/assets/9512131/21623321/17acc6ea-d202-11e6-80db-96033f95149c.png)

To fully utilize the advantages of stack driver's error reporting and logging, one has to use one of the client
libraries provided by google which are still in [beta](https://cloud.google.com/error-reporting/docs/setup/compute-engine#log_exceptions)
and not recommended for production purposes. 

We don't need to use the [beta](https://cloud.google.com/error-reporting/docs/setup/compute-engine#log_exceptions) client 
libraries provided by google. Fluentd log collector (alternative to Logstash in an ELK stack) is already pre installed
by the GKE in the Kubernetes cluster. In order to correctly utilize error reporting logs have to be formatted in specific formats like: 
- https://cloud.google.com/error-reporting/docs/formatting-error-messages
- an example implementation for sl4j/logback is [here](http://stackoverflow.com/questions/37420400/how-do-i-map-my-java-app-logging-events-to-corresponding-cloud-logging-event-lev)
- Further investigation should be done on all the functionailities of error reporting of the Stackdriver logging stack.
- An ELK stack could still be used instead of stack driver.


 

##Dynamic Configuration

Using the kubernetes [environment variables expanstion](http://kubernetes.io/docs/user-guide/configuring-containers/#environment-variables-and-variable-expansion) you can easily set the environment variables for every container. A typical use case is to add a set of environment variables for our `env: production` pods different than that of the `env: staging` pods. 

[*Secrets*](https://kubernetes.io/docs/user-guide/secrets/)
A Secret is an object that contains a small amount of sensitive data such as a password, a token, or a key. Such information might otherwise be put in a Pod specification or in an image; putting it in a Secret object allows for more control over how it is used, and reduces the risk of accidental exposure.
Users can create secrets, and the system also creates some secrets.

[*configmap*](https://kubernetes.io/docs/user-guide/configmap/)
The ConfigMap API resource holds key-value pairs of configuration data that can be consumed in pods or used to store configuration data for system components such as controllers. ConfigMap is similar to Secrets, but designed to more conveniently support working with strings that do not contain sensitive information.


##GAE vs GKE vs Self-managed Kubernetes
 
####GAE: Google App Engine
Google App Engine is a PAAS (**not** a container orchestration tool). It only provides a predefined stack of software per setup. This shouldn't be a solution 
since Google App Engine is a PAAS. It only provides a predefined stack of software per setup.Hardware and infrastructure
are managed by google. 
Features:
- Automatic scaling and load balancing
- Traffic splitting: Incremental deployments, easy rolling back
- No server maintenance
- Simply upload the application and run it
- Takes advantages of compute engine
- Background/ scheduled tasks.

Two types of environments with Google App Engine: Standard and Flexible.

**Standard**
 - Managed runtimes. (Java7, Python 2/7, Go and PHP) 
 - Can't write to Local file system 
 - *Kubernetes Cluster can't be configured*

**Flexible**
 - Java8, node.js, ruby and GO (not sandboxed)
 - ssh into instances
 - you can use docker containers
 - **Still Beta** and should not be used in production systems.
Read more about [here](https://cloud.google.com/appengine/docs/whatisgoogleappengine)

One more important distinction: projects running on App Engine can scale down to zero instances if no requests are 
coming in. This is extremely useful at the development stage as you can go for weeks without going over the generous 
free quota of instance-hours. Flexible runtime (i.e. "managed VMs") require at least one instance to run constantly.

####GKE: Google Container Engine

GKE, on the other hand, is an IAAS. It is the combination of Docker, Kubernetes and Google's expertise in cluster 
management and container orchestration.

#####Main features and advantages:

1. You don't need to spend time and effort managing kubernetes (leave that to google) and instead actually use it. Don't need 
   to worry about the Kubernetes master node going down. GKE guarantees master node’s uptime.
2. Easily instantiate a kubernetes installation with a custome configured cluster, with 1-click, through the google 
console UI.
3. Don't have to worry about etcd, how many nodes to fire up for a cluster, or whether it should be on the same node as 
kubernetes master. GKE guarntees etcd's uptime.
4. Managing authentication and authorization is much easier using the Google Cloud projects' IAM.
5. Cluster autoscaling **beta**
6. Without GKE to update Kubernetes, you would have to fireup a new cluster with latest kubernetes release and then move the pods
to the new cluster. However, with GKE its done with one command.
7. Heapster tool for monitoring is pre-installed with the GKE cluster. 
8. Stackdriver's monitoring and logging has support for cluster level logging. 

#####Docker Registries
Any private docker registry could be used in the kubernetes cluster to pull the docker images including nexus 3. [Here](https://blog.cloudhelix.io/using-a-private-docker-registry-with-kubernetes-f8d5f6b8f646#.56o8w13bp) is an example. However, having the nexus repository in an internal network that's not accessible publicly from the internet could be a problem. 
