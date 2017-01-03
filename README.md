# say-my-name
A spring-boot microservice to demonstrate docker container orchestration using a hosted kubernetes cluster on Google Cloud's Container Engine.

###About the sample microservice

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

####Useful commands
#####Manually setup a kubernetes .yaml deployment
```bash
kubectl --namespace=staging apply -f k8s/deployments/staging/
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
Through the Google console's[Stackdriver](https://app.google.stackdriver.com/monitoring/1039756/say-my-name-cpu-and-disk-usage?project=say-my-name-154414)
you can choose a label or a combination of labels to monitor the nodes your pods are running on, as seen in the screenshot below. You can monitor the CPU usage of all nodes running production pods by simply filtering by the label `env: production`
or aggregate the average CPU usage of nodes exposing the production frontend pods for example.
![screen shot 2017-01-03 at 21 06 48](https://cloud.githubusercontent.com/assets/9512131/21621291/a18d7cba-d1f8-11e6-9a02-8849eaad9160.png)

Using resource labels, you can filter the logs only coming from the components, apps, pods, nodes or clusters you wish 
as seen in the screenshot below.

![screen shot 2017-01-03 at 21 26 01](https://cloud.githubusercontent.com/assets/9512131/21623320/17abce16-d202-11e6-93c7-8708a6fe1e2a.png)
![screen shot 2017-01-03 at 21 25 30 1](https://cloud.githubusercontent.com/assets/9512131/21623321/17acc6ea-d202-11e6-80db-96033f95149c.png)

###Error Reporting and Logging
To fully utilize the advantages of stack driver's error reporting and logging, one has to use one of the client
libraries provided by google which are still in[beta](https://cloud.google.com/error-reporting/docs/setup/compute-engine#log_exceptions)
and not recommended for production purposes. 
 

##Dynamic Configuration


