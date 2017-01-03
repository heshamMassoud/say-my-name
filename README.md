# say-my-name
A spring-boot microservice to demonstrate docker container orchestration using a hosted kubernetes cluster on Google Cloud's Container Engine.


#####Manually setup a kubernetes .yaml deployment
```bash
kubectl --namespace=staging apply -f k8s/deployments/staging/
```

#####Manual horizontal scaling
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