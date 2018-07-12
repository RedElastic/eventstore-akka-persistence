package services.user

import domain.user.User.UserId
import domain.user.UserRepository

class UserService(userRepository: UserRepository) {

  def ban(userId: UserId) = {
    userRepository.ban(userId)
  }
}