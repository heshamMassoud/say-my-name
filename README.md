# say-my-name
A spring-boot microservice to demonstrate docker container orchestration using a hosted kubernetes cluster on Google Cloud's Container Engine.

###About the sample microservice
Production: http://104.155.85.38/say/there

Staging: http://130.211.93.195/say/there


###What the Prototype Covers
1. A deployed instance of Jenkins server on a Kubernetes cluster.
2. Setup of a multi-branched pipeline on Jenkins.
3. Setup of a production and staging environment for the microservice on the kubernetes cluster.
4. Development workflow and release to production (and how to switch it to canary-releasing).
5. Scaling (Manual horizontal scaling, vertical and auto-scaling)
6. Monitoring and logging using Stackdriver.
7. Dynamic Configuration.
8. Advantages of using Kubernetes and/or Kubernetes on GKE.

###Bird's Eye View

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
####Jenkins Multi-branched Pipeline
<img width="880" alt="screen shot 2017-01-03 at 19 12 56" src="https://cloud.githubusercontent.com/assets/9512131/21618084/80643c54-d1e9-11e6-9c28-265f714081bc.png">

####Useful commands
#####Manually setup a kubernetes .yaml deployment
```bash
kubectl --namespace=staging apply -f k8s/deployments/staging/
```
#####Manual horizontal scaling of production environment pods
```bash
kubectl --namespace=production scale deployment say-my-name-frontend-production --replicas=4
```
#####Status of created services and pods
```bash
kubectl --namespace="staging" get services
kubectl --namespace="staging" get pods
```
#####Details of created services and pods
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
Through the Google console's [Stackdriver](https://app.google.stackdriver.com/monitoring/1039756/say-my-name-cpu-and-disk-usage?project=say-my-name-154414)
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
 

##Dynamic Configuration

Using the kubernetes [environment variables expanstion](http://kubernetes.io/docs/user-guide/configuring-containers/#environment-variables-and-variable-expansion) you can easily set the environment variables for every container. A typical use case is to add a set of environment variables for our `env: production` pods different than that of the `env: staging` pods. 
