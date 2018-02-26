#!/bin/bash

sudo ansible-playbook -i "$1," test.yml \
-e 'host_key_checking=False' \
--extra-vars="{SERVICE: [$2]}" \
--private-key=/path/to/rsa/key \
-e 'ansible_ssh_user=user' \