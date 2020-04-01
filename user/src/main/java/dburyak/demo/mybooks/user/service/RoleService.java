package dburyak.demo.mybooks.user.service;

import dburyak.demo.mybooks.domain.Permission;
import dburyak.demo.mybooks.user.domain.Role;
import dburyak.demo.mybooks.user.repository.RolesRepository;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class RoleService {

    @Inject
    private RolesRepository rolesRepository;

    public Flowable<Permission> getPermissionsOfRoleNames(Set<String> roles) {
        return rolesRepository.findAllByNames(roles)
                .flatMap(r -> Flowable.fromIterable(r.getPermissions()))
                .distinct();
    }

    public Flowable<Permission> getPermissionsOfRoleNames(List<String> roles) {
        return getPermissionsOfRoleNames(new HashSet<>(roles));
    }

    public Flowable<Permission> getPermissionsOfRoleName(String roleName) {
        return rolesRepository.findByName(roleName)
                .flatMapPublisher(r -> Flowable.fromIterable(r.getPermissions()))
                .distinct();
    }

    public Flowable<Permission> getPermissionsOfRoles(Set<Role> roles) {
        return getPermissionsOfRoleNames(roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
    }

    public Flowable<Permission> getPermissionsOfRoles(List<Role> roles) {
        return getPermissionsOfRoles(new HashSet<>(roles));
    }

    public Flowable<Permission> getPermissionsOfRole(Role role) {
        return getPermissionsOfRoleName(role.getName());
    }

    public Flowable<Role> allRoles() {
        return rolesRepository.list();
    }

    public Flowable<Role> allSystemRoles() {
        return rolesRepository.findAllByIsSystem(true);
    }

    public Flowable<Role> allNonSystemRoles() {
        return rolesRepository.findAllByIsSystem(false);
    }

    public Maybe<String> save(Role role) {
        return rolesRepository.save(role);
    }

    public Maybe<String> save(String name, boolean isSystem, Set<Permission> permissions) {
        return rolesRepository.save(name, isSystem, permissions);
    }
}
