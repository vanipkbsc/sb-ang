# Using a PersistentVolumeClaim (PVC) for Tekton Pipeline

To use a PersistentVolumeClaim (PVC) for your Tekton Pipeline (for example, with `git-clone` tasks), you do **not** need to manually create a PersistentVolume because your cluster already has the `standard` StorageClass with the `rancher.io/local-path` dynamic provisioner. This means Kubernetes should automatically create a PersistentVolume when you create a PVC.

However, your PVC may be stuck in `Pending`. Let's fix this step by step:

---

## 1. Check the local-path-provisioner Pod

Make sure the local-path-provisioner pod is running:

```sh
kubectl get pods -n kube-system | grep local-path
```

If you see a pod and it is `Running`, that's good.  
If not, reinstall the provisioner:

```sh
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
```

---

## 2. Check PVC Events

Describe your PVC to see why it's pending:

```sh
kubectl describe pvc my-pvc
```

Look for any error messages at the bottom (Events section).  
If you see something like "no nodes available to schedule pods", it may be a node label or taint issue.

---

## 3. PVC YAML Example

Your PVC YAML is correct for dynamic provisioning:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  storageClassName: standard
```

Apply it:

```sh
kubectl apply -f /workspaces/sb-ang/kubernetes-stuff/my-pvc.yaml
```

---

## 4. Use PVC in Tekton Pipeline

In your Tekton PipelineRun or TaskRun, reference the PVC like this:

```yaml
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  name: example-pipelinerun
spec:
  pipelineRef:
    name: your-pipeline
  workspaces:
    - name: shared-data
      persistentVolumeClaim:
        claimName: my-pvc
```

And in your Pipeline/Task, use a workspace named `shared-data`.

---

## 5. If Still Pending

- Check the output of `kubectl describe pvc my-pvc` and share any warning/error events.
- Make sure your node has enough disk space.
- Make sure the local-path-provisioner pod is running and healthy.

---

## Summary

- Your setup should work with the current StorageClass.
- If the PVC is still pending, the issue is likely with the local-path-provisioner pod or node disk space.
- Use the PVC in Tekton by referencing it in your PipelineRun/TaskRun as shown above.

---

## PVC Pending: WaitForFirstConsumer

If your PVC is in `Pending` with the event:

```
Normal  WaitForFirstConsumer  ...  waiting for first consumer to be created before binding
```

This is **expected behavior** when using the `WaitForFirstConsumer` volume binding mode (which is default for many dynamic provisioners).

**It means:**  
The PersistentVolume will only be created and bound when a Pod (or Tekton TaskRun/PipelineRun) actually tries to use the PVC.

---

### What to do next?

Just reference this PVC in your Tekton PipelineRun/TaskRun workspace.  
When Tekton creates a Pod that uses the PVC, Kubernetes will provision the volume.

#### Example: Use PVC in Tekton PipelineRun

```yaml
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  name: example-pipelinerun
spec:
  pipelineRef:
    name: your-pipeline
  workspaces:
    - name: shared-data
      persistentVolumeClaim:
        claimName: my-pvc
```

And in your Pipeline/Task, use a workspace named `shared-data`.

---

### Summary

- Your PVC is waiting for a Pod to use it.  
- This is normal.  
- When your Tekton PipelineRun/TaskRun starts and uses the PVC, it will be bound and provisioned automatically.
- You do not need to do anything else.  
- Just use the PVC in your Tekton resources!

---

## TaskRun Example: git-clone

Here’s a **TaskRun** YAML to use the `git-clone` Tekton task to clone your repo (`https://github.com/simeptk/sb-ang.git`, branch `main`).  
This example uses a workspace named `output` and assumes you have a PVC named `my-pvc`:

```yaml
apiVersion: tekton.dev/v1
kind: TaskRun
metadata:
  name: git-clone-run
spec:
  taskRef:
    name: git-clone
  params:
    - name: url
      value: https://github.com/simeptk/sb-ang.git
    - name: revision
      value: main
    - name: deleteExisting
      value: "true"
  workspaces:
    - name: output
      persistentVolumeClaim:
        claimName: my-pvc
```

**How to use:**

1. Save this YAML to a file, e.g. `git-clone-taskrun.yaml`.
2. Apply it:
   ```sh
   kubectl apply -f git-clone-taskrun.yaml
   ```
3. Check status:
   ```sh
   kubectl get taskrun
   kubectl describe taskrun git-clone-run
   ```

**Note:**  
- The workspace name (`output`) must match what the `git-clone` task expects (usually `output`).
- The PVC (`my-pvc`) will be bound and used for the workspace.

---

## TaskRun Succeeded

Your `git-clone-run` **TaskRun succeeded** and cloned your repository (`https://github.com/simeptk/sb-ang.git`, branch `main`) into the PVC (`my-pvc`) as expected.

**Key points:**
- `Status: Succeeded`
- The commit SHA and repo URL are recorded in the TaskRun results.
- The workspace `output` (backed by your PVC) now contains the cloned repository.

---

### Next Steps

- You can mount the same workspace (`output`/`my-pvc`) in subsequent Tekton tasks to use the cloned code.
- If you want to see the files, you can run a debug pod that mounts the same PVC:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: debug-pvc
spec:
  containers:
    - name: shell
      image: busybox
      command: ["sleep", "3600"]
      volumeMounts:
        - name: my-volume
          mountPath: /data
  volumes:
    - name: my-volume
      persistentVolumeClaim:
        claimName: my-pvc
  restartPolicy: Never
```

Apply and then:

```sh
kubectl exec -it debug-pvc -- sh
ls /data
```

---

### Summary

- Your Tekton `git-clone` TaskRun is working.
- You can now chain more tasks in a Pipeline using the same workspace, or inspect the PVC as shown above.
- Let me know if you want a full Pipeline example or help with the next step!