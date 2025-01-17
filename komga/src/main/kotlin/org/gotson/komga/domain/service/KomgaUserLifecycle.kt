package org.gotson.komga.domain.service

import mu.KotlinLogging
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.UserEmailAlreadyExistsException
import org.gotson.komga.domain.persistence.AuthenticationActivityRepository
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.persistence.ReadProgressRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

private val logger = KotlinLogging.logger {}

@Service
class KomgaUserLifecycle(
  private val userRepository: KomgaUserRepository,
  private val readProgressRepository: ReadProgressRepository,
  private val authenticationActivityRepository: AuthenticationActivityRepository,
  private val passwordEncoder: PasswordEncoder,
  private val sessionRegistry: SessionRegistry,
  private val transactionTemplate: TransactionTemplate,

) : UserDetailsService {

  override fun loadUserByUsername(username: String): UserDetails =
    userRepository.findByEmailIgnoreCaseOrNull(username)?.let {
      KomgaPrincipal(it)
    } ?: throw UsernameNotFoundException(username)

  fun updatePassword(user: KomgaUser, newPassword: String, expireSessions: Boolean) {
    logger.info { "Changing password for user ${user.email}" }
    val updatedUser = user.copy(password = passwordEncoder.encode(newPassword))
    userRepository.update(updatedUser)

    if (expireSessions) expireSessions(updatedUser)
  }

  fun countUsers() = userRepository.count()

  @Throws(UserEmailAlreadyExistsException::class)
  fun createUser(komgaUser: KomgaUser): KomgaUser {
    if (userRepository.existsByEmailIgnoreCase(komgaUser.email)) throw UserEmailAlreadyExistsException("A user with the same email already exists: ${komgaUser.email}")

    userRepository.insert(komgaUser.copy(password = passwordEncoder.encode(komgaUser.password)))

    val createdUser = userRepository.findByIdOrNull(komgaUser.id)!!
    logger.info { "User created: $createdUser" }
    return createdUser
  }

  fun deleteUser(user: KomgaUser) {
    logger.info { "Deleting user: $user" }

    transactionTemplate.executeWithoutResult {
      readProgressRepository.deleteByUserId(user.id)
      authenticationActivityRepository.deleteByUser(user)
      userRepository.delete(user.id)
    }

    expireSessions(user)
  }

  private fun expireSessions(user: KomgaUser) {
    logger.info { "Expiring all sessions for user: ${user.email}" }
    sessionRegistry.allPrincipals
      .filterIsInstance<KomgaPrincipal>()
      .filter { it.user.id == user.id }
      .flatMap { sessionRegistry.getAllSessions(it, false) }
      .forEach {
        logger.info { "Expiring session: ${it.sessionId}" }
        it.expireNow()
      }
  }
}
