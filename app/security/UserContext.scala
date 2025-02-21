package security


case class UserContext(
                        userId: Option[Long],
                        roles: Seq[Role],
                        sessionId: Option[String] = None,
                        timestamp: Long = System.currentTimeMillis(),
                        expiresAt: Long
                      ) {
  def isAdmin: Boolean = roles.contains(Role.ADMIN)
  def hasRole(role: Role): Boolean = roles.contains(role)
}