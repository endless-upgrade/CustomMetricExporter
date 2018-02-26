#!/bin/bash

base=`dirname $0`

sudo ansible-playbook -i "$1," "$base/test-service.yml" \
-e 'host_key_checking=False' \
--extra-vars="{SERVICE: [$2]}" \
--private-key=key \
-e 'ansible_ssh_user=user' \