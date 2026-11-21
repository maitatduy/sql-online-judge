import { execFileSync } from "node:child_process";

const MYSQL_CONTAINER = process.env.E2E_MYSQL_CONTAINER ?? "sqljudge-dev-mysql-1";
const EMAIL_PATTERN = /^[a-z0-9.]+@sqljudge\.test$/;

export type GrantedRole = "SETTER" | "ADMIN";

export function grantRole(email: string, role: GrantedRole): void {
    if (!EMAIL_PATTERN.test(email)) {
        throw new Error(`Chỉ nâng vai trò cho tài khoản thử nghiệm: ${email}`);
    }
    execFileSync("docker", [
        "exec",
        MYSQL_CONTAINER,
        "sh",
        "-c",
        `mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "UPDATE sqljudge_auth.users SET role = '${role}' WHERE email = '${email}'" 2>/dev/null`,
    ]);
}
